package ma.siblhish.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.mapper.EntityMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    @PersistenceContext
    private EntityManager entityManager;
    private final EntityMapper mapper;

    /**
     * Obtenir les dépenses par catégorie dans une plage de dates
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     */
    public StatisticsDto getExpensesByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                c.id as category_id,
                c.name as category_name,
                c.icon as category_icon,
                c.color as category_color,
                COALESCE(SUM(e.amount), 0) as total_amount,
                COUNT(e.id) as transaction_count
            FROM categories c
            LEFT JOIN expenses e ON c.id = e.category_id 
                AND e.user_id = :userId 
                AND DATE(e.creation_date) >= :startDate 
                AND DATE(e.creation_date) <= :endDate
            GROUP BY c.id, c.name, c.icon, c.color
            HAVING COALESCE(SUM(e.amount), 0) > 0
            ORDER BY total_amount DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        // Calculer le total pour les pourcentages
        double totalAmount = results.stream()
                .mapToDouble(row -> mapper.convertToDouble(row[4]))
                .sum();

        List<CategoryExpenseDto> categories = new ArrayList<>();
        for (Object[] row : results) {
            double amount = mapper.convertToDouble(row[4]);
            CategoryExpenseDto dto = new CategoryExpenseDto();
            dto.setCategoryId(((Number) row[0]).longValue());
            dto.setCategoryName((String) row[1]);
            dto.setIcon((String) row[2]);
            dto.setColor((String) row[3]);
            dto.setAmount(amount);
            dto.setPercentage(totalAmount > 0 ? (amount / totalAmount) * 100 : 0);
            categories.add(dto);
        }

        return new StatisticsDto(totalAmount, categories);
    }

    /**
     * Obtenir les revenus et dépenses par période
     * La granularité est déterminée automatiquement selon la plage de dates :
     * - daily (1 jour) : agrégation par jour
     * - weekly (7 jours) : agrégation par jour pour voir chaque jour de la semaine
     * - monthly (30 jours) : agrégation par jour pour voir chaque jour du mois
     * - 3months (90 jours) : agrégation par mois pour voir chaque mois (3 points)
     * - 6months (180 jours) : agrégation par mois pour voir chaque mois
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     */
    public List<PeriodSummaryDto> getPeriodSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        // Déterminer la granularité selon la plage de dates
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        String periodFormat;
        
        if (daysBetween <= 1) {
            // daily : 1 jour → agrégation par jour (même si c'est 1 seul jour)
            periodFormat = "TO_CHAR(creation_date, 'YYYY-MM-DD')";
        } else if (daysBetween <= 31) {
            // weekly (7 jours) ou monthly (30 jours) → agrégation par jour
            // Pour voir chaque jour de la semaine/mois
            periodFormat = "TO_CHAR(creation_date, 'YYYY-MM-DD')";
        } else {
            // 3months (~90 jours) ou 6months (~180 jours) ou plus → agrégation par mois
            // Pour voir chaque mois (3 points pour 3 mois, 6 points pour 6 mois)
            periodFormat = "TO_CHAR(creation_date, 'YYYY-MM')";
        }

        String sql = String.format("""
            SELECT 
                period,
                COALESCE(SUM(total_income), 0) as total_income,
                COALESCE(SUM(total_expenses), 0) as total_expenses,
                COALESCE(SUM(total_income), 0) - COALESCE(SUM(total_expenses), 0) as balance
            FROM (
                SELECT 
                    %s as period,
                    amount as total_income,
                    0 as total_expenses
                FROM incomes
                WHERE user_id = :userId 
                    AND DATE(creation_date) >= :startDate 
                    AND DATE(creation_date) <= :endDate
                UNION ALL
                SELECT 
                    %s as period,
                    0 as total_income,
                    amount as total_expenses
                FROM expenses
                WHERE user_id = :userId 
                    AND DATE(creation_date) >= :startDate 
                    AND DATE(creation_date) <= :endDate
            ) combined
            GROUP BY period
            ORDER BY period
        """, periodFormat, periodFormat);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<PeriodSummaryDto> summaries = new ArrayList<>();
        for (Object[] row : results) {
            PeriodSummaryDto dto = new PeriodSummaryDto();
            dto.setPeriod((String) row[0]); // Le champ "period" contient la période formatée
            dto.setTotalIncome(mapper.convertToDouble(row[1]));
            dto.setTotalExpenses(mapper.convertToDouble(row[2]));
            dto.setBalance(mapper.convertToDouble(row[3]));
            summaries.add(dto);
        }

        return summaries;
    }

    /**
     * Budget vs Réel : Compare le budget prévu avec les dépenses réelles par catégorie
     * Optimisé avec CTEs pour éviter les sous-requêtes corrélées
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     */
    public List<BudgetVsActualDto> getBudgetVsActual(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                COALESCE(b.category_id, 0) as category_id,
                COALESCE(c.name, 'Budget Global') as category_name,
                COALESCE(c.icon, '') as category_icon,
                COALESCE(c.color, '#9E9E9E') as category_color,
                SUM(b.amount) as budget_amount,
                SUM(COALESCE(e.amount, 0)) as actual_amount
            FROM budgets b
            LEFT JOIN categories c ON b.category_id = c.id
            LEFT JOIN expenses e ON e.user_id = :userId
              AND DATE(e.creation_date) >= GREATEST(DATE(b.start_date), :startDate)
              AND DATE(e.creation_date) <= LEAST(DATE(b.end_date), :endDate)
              AND (b.category_id IS NULL OR e.category_id = b.category_id)
            WHERE b.user_id = :userId
              AND DATE(b.start_date) <= :endDate
              AND DATE(b.end_date) >= :startDate
            GROUP BY b.category_id, c.name, c.icon, c.color
            HAVING SUM(b.amount) > 0
            ORDER BY budget_amount DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<BudgetVsActualDto> data = new ArrayList<>();
        for (Object[] row : results) {
            BudgetVsActualDto dto = new BudgetVsActualDto();
            Long categoryId = row[0] != null && ((Number) row[0]).longValue() != 0 ? 
                ((Number) row[0]).longValue() : null;
            dto.setCategoryId(categoryId);
            dto.setCategoryName((String) row[1]);
            dto.setIcon((String) row[2]);
            dto.setColor((String) row[3]);
            
            Double budgetAmount = mapper.convertToDouble(row[4]);
            Double actualAmount = mapper.convertToDouble(row[5]);
            
            dto.setBudgetAmount(budgetAmount);
            dto.setActualAmount(actualAmount);
            dto.setDifference(budgetAmount - actualAmount);
            dto.setPercentageUsed(budgetAmount > 0 ? (actualAmount / budgetAmount) * 100 : 0.0);
            
            data.add(dto);
        }

        return data;
    }

    /**
     * Top Catégories Budgétisées : Liste les catégories avec les budgets les plus importants
     * Optimisé avec jointures directes
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     */
    public List<TopBudgetCategoryDto> getTopBudgetCategories(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                COALESCE(b.category_id, 0) as category_id,
                COALESCE(c.name, 'Budget Global') as category_name,
                COALESCE(c.icon, '') as category_icon,
                COALESCE(c.color, '#9E9E9E') as category_color,
                SUM(b.amount) as budget_amount,
                SUM(COALESCE(e.amount, 0)) as spent_amount
            FROM budgets b
            LEFT JOIN categories c ON b.category_id = c.id
            LEFT JOIN expenses e ON e.user_id = :userId
              AND DATE(e.creation_date) >= GREATEST(DATE(b.start_date), :startDate)
              AND DATE(e.creation_date) <= LEAST(DATE(b.end_date), :endDate)
              AND (b.category_id IS NULL OR e.category_id = b.category_id)
            WHERE b.user_id = :userId
              AND DATE(b.start_date) <= :endDate
              AND DATE(b.end_date) >= :startDate
            GROUP BY b.category_id, c.name, c.icon, c.color
            HAVING SUM(b.amount) > 0
            ORDER BY budget_amount DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TopBudgetCategoryDto> data = new ArrayList<>();
        for (Object[] row : results) {
            TopBudgetCategoryDto dto = new TopBudgetCategoryDto();
            Long categoryId = row[0] != null && ((Number) row[0]).longValue() != 0 ? 
                ((Number) row[0]).longValue() : null;
            dto.setCategoryId(categoryId);
            dto.setCategoryName((String) row[1]);
            dto.setIcon((String) row[2]);
            dto.setColor((String) row[3]);
            
            Double budgetAmount = mapper.convertToDouble(row[4]);
            Double spentAmount = mapper.convertToDouble(row[5]);
            
            dto.setBudgetAmount(budgetAmount);
            dto.setSpentAmount(spentAmount);
            dto.setRemainingAmount(budgetAmount - spentAmount);
            dto.setPercentageUsed(budgetAmount > 0 ? (spentAmount / budgetAmount) * 100 : 0.0);
            
            data.add(dto);
        }

        return data;
    }

    /**
     * Efficacité Budgétaire : Mesure globale de l'efficacité des budgets
     * Optimisé avec jointures directes uniquement (vue dérivée au lieu de CTE)
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     */
    public BudgetEfficiencyDto getBudgetEfficiency(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                COUNT(*) as total_budgets,
                SUM(budget_summary.amount) as total_budget_amount,
                SUM(budget_summary.spent_amount) as total_spent_amount,
                SUM(CASE WHEN budget_summary.spent_amount <= budget_summary.amount THEN 1 ELSE 0 END) as budgets_on_track,
                SUM(CASE WHEN budget_summary.spent_amount > budget_summary.amount THEN 1 ELSE 0 END) as budgets_exceeded
            FROM (
                SELECT 
                    b.id,
                    b.amount,
                    SUM(COALESCE(e.amount, 0)) as spent_amount
                FROM budgets b
                LEFT JOIN expenses e ON e.user_id = :userId
                  AND DATE(e.creation_date) >= GREATEST(DATE(b.start_date), :startDate)
                  AND DATE(e.creation_date) <= LEAST(DATE(b.end_date), :endDate)
                  AND (b.category_id IS NULL OR e.category_id = b.category_id)
                WHERE b.user_id = :userId
                  AND DATE(b.start_date) <= :endDate
                  AND DATE(b.end_date) >= :startDate
                GROUP BY b.id, b.amount
            ) budget_summary
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        BudgetEfficiencyDto dto = new BudgetEfficiencyDto();
        if (!results.isEmpty()) {
            Object[] row = results.get(0);
            
            Integer totalBudgets = ((Number) row[0]).intValue();
            Double totalBudgetAmount = mapper.convertToDouble(row[1]);
            Double totalSpentAmount = mapper.convertToDouble(row[2]);
            Integer budgetsOnTrack = ((Number) row[3]).intValue();
            Integer budgetsExceeded = ((Number) row[4]).intValue();
            
            dto.setTotalBudgets(totalBudgets);
            dto.setTotalBudgetAmount(totalBudgetAmount);
            dto.setTotalSpentAmount(totalSpentAmount);
            dto.setTotalRemainingAmount(totalBudgetAmount - totalSpentAmount);
            dto.setAveragePercentageUsed(totalBudgetAmount > 0 ? (totalSpentAmount / totalBudgetAmount) * 100 : 0.0);
            dto.setBudgetsOnTrack(budgetsOnTrack);
            dto.setBudgetsExceeded(budgetsExceeded);
        } else {
            dto.setTotalBudgets(0);
            dto.setTotalBudgetAmount(0.0);
            dto.setTotalSpentAmount(0.0);
            dto.setTotalRemainingAmount(0.0);
            dto.setAveragePercentageUsed(0.0);
            dto.setBudgetsOnTrack(0);
            dto.setBudgetsExceeded(0);
        }

        return dto;
    }

    /**
     * Tendance Mensuelle Budgets : Évolution des budgets sur plusieurs mois
     * Optimisé avec CTEs pour éviter les sous-requêtes corrélées
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     */
    public List<MonthlyBudgetTrendDto> getMonthlyBudgetTrend(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                TO_CHAR(DATE(b.start_date), 'YYYY-MM') as month,
                COUNT(DISTINCT b.id) as budget_count,
                SUM(b.amount) as total_budget_amount,
                SUM(COALESCE(e.amount, 0)) as total_spent_amount
            FROM budgets b
            LEFT JOIN expenses e ON e.user_id = :userId
              AND DATE(e.creation_date) >= GREATEST(DATE(b.start_date), :startDate)
              AND DATE(e.creation_date) <= LEAST(DATE(b.end_date), :endDate)
              AND (b.category_id IS NULL OR e.category_id = b.category_id)
            WHERE b.user_id = :userId
              AND DATE(b.start_date) <= :endDate
              AND DATE(b.end_date) >= :startDate
            GROUP BY TO_CHAR(DATE(b.start_date), 'YYYY-MM')
            ORDER BY month
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<MonthlyBudgetTrendDto> data = new ArrayList<>();
        for (Object[] row : results) {
            MonthlyBudgetTrendDto dto = new MonthlyBudgetTrendDto();
            dto.setMonth((String) row[0]);
            dto.setBudgetCount(((Number) row[1]).intValue());
            
            Double totalBudgetAmount = mapper.convertToDouble(row[2]);
            Double totalSpentAmount = mapper.convertToDouble(row[3]);
            
            dto.setTotalBudgetAmount(totalBudgetAmount);
            dto.setTotalSpentAmount(totalSpentAmount);
            dto.setAveragePercentageUsed(totalBudgetAmount > 0 ? (totalSpentAmount / totalBudgetAmount) * 100 : 0.0);
            
            data.add(dto);
        }

        return data;
    }

    /**
     * Répartition des Budgets : Répartition du budget total par catégorie (pour pie chart)
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     */
    public List<BudgetDistributionDto> getBudgetDistribution(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                COALESCE(b.category_id, 0) as category_id,
                COALESCE(c.name, 'Budget Global') as category_name,
                COALESCE(c.icon, '') as category_icon,
                COALESCE(c.color, '#9E9E9E') as category_color,
                COALESCE(SUM(b.amount), 0) as budget_amount
            FROM budgets b
            LEFT JOIN categories c ON b.category_id = c.id
            WHERE b.user_id = :userId
              AND DATE(b.start_date) <= :endDate
              AND DATE(b.end_date) >= :startDate
            GROUP BY b.category_id, c.name, c.icon, c.color
            HAVING COALESCE(SUM(b.amount), 0) > 0
            ORDER BY budget_amount DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        // Calculer le total pour les pourcentages
        double totalBudgetAmount = results.stream()
                .mapToDouble(row -> mapper.convertToDouble(row[4]))
                .sum();

        List<BudgetDistributionDto> data = new ArrayList<>();
        for (Object[] row : results) {
            BudgetDistributionDto dto = new BudgetDistributionDto();
            Long categoryId = row[0] != null && ((Number) row[0]).longValue() != 0 ? 
                ((Number) row[0]).longValue() : null;
            dto.setCategoryId(categoryId);
            dto.setCategoryName((String) row[1]);
            dto.setIcon((String) row[2]);
            dto.setColor((String) row[3]);
            
            Double budgetAmount = mapper.convertToDouble(row[4]);
            dto.setBudgetAmount(budgetAmount);
            dto.setPercentage(totalBudgetAmount > 0 ? (budgetAmount / totalBudgetAmount) * 100 : 0.0);
            
            data.add(dto);
        }

        return data;
    }
}

