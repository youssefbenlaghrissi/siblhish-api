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
     * Requête unifiée pour récupérer toutes les données budgets par catégorie
     * Cette méthode privée est utilisée par toutes les méthodes de statistiques budgets
     * pour éviter de faire plusieurs requêtes SQL similaires
     */
    private List<Object[]> getBudgetStatisticsData(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                b.category_id,
                c.name as category_name,
                c.icon as category_icon,
                c.color as category_color,
                SUM(b.amount) as budget_amount,
                SUM(COALESCE(e.amount, 0)) as actual_amount
            FROM budgets b
            LEFT JOIN categories c ON b.category_id = c.id
            LEFT JOIN expenses e ON e.user_id = :userId
              AND DATE(e.creation_date) >= GREATEST(DATE(b.start_date), :startDate)
              AND DATE(e.creation_date) <= LEAST(DATE(b.end_date), :endDate)
              AND e.category_id = b.category_id
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
        return results;
    }

    /**
     * Budget vs Réel : Compare le budget prévu avec les dépenses réelles par catégorie
     * Utilise la requête unifiée pour optimiser les performances
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     */
    public List<BudgetVsActualDto> getBudgetVsActual(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = getBudgetStatisticsData(userId, startDate, endDate);

        List<BudgetVsActualDto> data = new ArrayList<>();
        for (Object[] row : results) {
            BudgetVsActualDto dto = new BudgetVsActualDto();
            Long categoryId = row[0] != null ? ((Number) row[0]).longValue() : null;
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
     * Efficacité Budgétaire : Mesure globale de l'efficacité des budgets
     * Utilise la requête unifiée pour optimiser les performances
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     */
    public BudgetEfficiencyDto getBudgetEfficiency(Long userId, LocalDate startDate, LocalDate endDate) {
        // Récupérer les données par budget individuel pour calculer on_track/exceeded
        String sql = """
            SELECT 
                b.id,
                b.amount,
                SUM(COALESCE(e.amount, 0)) as spent_amount
            FROM budgets b
            LEFT JOIN expenses e ON e.user_id = :userId
              AND DATE(e.creation_date) >= GREATEST(DATE(b.start_date), :startDate)
              AND DATE(e.creation_date) <= LEAST(DATE(b.end_date), :endDate)
              AND e.category_id = b.category_id
            WHERE b.user_id = :userId
              AND DATE(b.start_date) <= :endDate
              AND DATE(b.end_date) >= :startDate
            GROUP BY b.id, b.amount
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> budgetResults = query.getResultList();

        // Utiliser les données unifiées pour les totaux
        List<Object[]> categoryResults = getBudgetStatisticsData(userId, startDate, endDate);

        BudgetEfficiencyDto dto = new BudgetEfficiencyDto();
        
        // Calculer les totaux depuis les données par catégorie
        int totalBudgets = budgetResults.size();
        double totalBudgetAmount = categoryResults.stream()
                .mapToDouble(row -> mapper.convertToDouble(row[4]))
                .sum();
        double totalSpentAmount = categoryResults.stream()
                .mapToDouble(row -> mapper.convertToDouble(row[5]))
                .sum();
        
        // Calculer budgets on track et exceeded depuis les données par budget
        int budgetsOnTrack = 0;
        int budgetsExceeded = 0;
        for (Object[] row : budgetResults) {
            Double budgetAmount = mapper.convertToDouble(row[1]);
            Double spentAmount = mapper.convertToDouble(row[2]);
            if (spentAmount <= budgetAmount) {
                budgetsOnTrack++;
            } else {
                budgetsExceeded++;
            }
        }
        
        dto.setTotalBudgets(totalBudgets);
        dto.setTotalBudgetAmount(totalBudgetAmount);
        dto.setTotalSpentAmount(totalSpentAmount);
        dto.setTotalRemainingAmount(totalBudgetAmount - totalSpentAmount);
        dto.setAveragePercentageUsed(totalBudgetAmount > 0 ? (totalSpentAmount / totalBudgetAmount) * 100 : 0.0);
        dto.setBudgetsOnTrack(budgetsOnTrack);
        dto.setBudgetsExceeded(budgetsExceeded);

        return dto;
    }

    /**
     * Répartition des Budgets : Répartition du budget total par catégorie (pour pie chart)
     * Utilise la requête unifiée pour optimiser les performances
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     */
    public List<BudgetDistributionDto> getBudgetDistribution(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = getBudgetStatisticsData(userId, startDate, endDate);

        // Calculer le total pour les pourcentages
        double totalBudgetAmount = results.stream()
                .mapToDouble(row -> mapper.convertToDouble(row[4]))
                .sum();

        List<BudgetDistributionDto> data = new ArrayList<>();
        for (Object[] row : results) {
            BudgetDistributionDto dto = new BudgetDistributionDto();
            Long categoryId = row[0] != null ? ((Number) row[0]).longValue() : null;
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

