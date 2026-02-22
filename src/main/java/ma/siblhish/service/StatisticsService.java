package ma.siblhish.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.mapper.EntityMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
     * Optimisé : utilise creation_date directement (sans DATE()) pour utiliser les index
     * et calcule le total directement en SQL avec SUM() OVER ()
     *
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     */
    public CategoryExpensesDto getExpensesByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        // Convertir LocalDate en LocalDateTime pour utiliser les index sur creation_date
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        String sql = """
            SELECT 
                c.id as category_id,
                c.name as category_name,
                c.icon as category_icon,
                c.color as category_color,
                SUM(e.amount) as total_amount,
                COUNT(e.id) as transaction_count,
                SUM(SUM(e.amount)) OVER () as grand_total
            FROM categories c
            LEFT JOIN expenses e ON c.id = e.category_id 
                AND e.user_id = :userId 
                AND e.deleted = false
                AND e.creation_date >= :startDateTime 
                AND e.creation_date <= :endDateTime
            GROUP BY c.id, c.name, c.icon, c.color
            HAVING SUM(e.amount) > 0
            ORDER BY total_amount DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("startDateTime", startDateTime);
        query.setParameter("endDateTime", endDateTime);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        // Récupérer le total depuis la première ligne (grand_total)
        double totalAmount = results.isEmpty() ? 0.0 :
                mapper.convertToDouble(results.get(0)[6]);

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

        return new CategoryExpensesDto(totalAmount, categories);
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
        // Convertir LocalDate en LocalDateTime pour utiliser les index sur creation_date
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

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

        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT 
                period,
                SUM(total_income) as total_income,
                SUM(total_expenses) as total_expenses,
                SUM(total_income) - SUM(total_expenses) as balance
            FROM (
                SELECT 
            """);
        sqlBuilder.append(periodFormat).append(" as period, ");
        sqlBuilder.append("""
                    amount as total_income,
                    0 as total_expenses
                FROM incomes
                WHERE user_id = :userId 
                    AND deleted = false
                    AND creation_date >= :startDateTime 
                    AND creation_date <= :endDateTime
                UNION ALL
                SELECT 
            """);
        sqlBuilder.append(periodFormat).append(" as period, ");
        sqlBuilder.append("""
                    0 as total_income,
                    amount as total_expenses
                FROM expenses
                WHERE user_id = :userId 
                    AND deleted = false
                    AND creation_date >= :startDateTime 
                    AND creation_date <= :endDateTime
            ) combined
            GROUP BY period
            ORDER BY period
        """);
        String sql = sqlBuilder.toString();

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("startDateTime", startDateTime);
        query.setParameter("endDateTime", endDateTime);

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
     * Utilisée par getAllBudgetStatisticsUnified() pour éviter les requêtes SQL dupliquées
     * Optimisé : utilise creation_date directement (sans DATE()) et optimise GREATEST/LEAST
     */
    private List<Object[]> getBudgetStatisticsData(Long userId, LocalDate startDate, LocalDate endDate) {
        // Convertir LocalDate en LocalDateTime pour utiliser les index
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Optimisé : un budget par catégorie via (category_id, max_id) puis join sur PK
        // Index recommandé : budgets(user_id, deleted, start_date, end_date, category_id, id)
        String sql = """
            SELECT 
                b.category_id,
                c.name as category_name,
                c.icon as category_icon,
                c.color as category_color,
                b.amount as budget_amount,
                COALESCE(SUM(e.amount), 0) as actual_amount
            FROM (
                SELECT category_id, user_id, MAX(id) as max_id
                FROM budgets
                WHERE user_id = :userId
                  AND deleted = false
                  AND start_date <= :endDate
                  AND end_date >= :startDate
                GROUP BY category_id, user_id
            ) pick
            INNER JOIN budgets b ON b.category_id = pick.category_id AND b.user_id = pick.user_id AND b.id = pick.max_id
            LEFT JOIN categories c ON b.category_id = c.id
            LEFT JOIN expenses e ON e.user_id = b.user_id
              AND e.deleted = false
              AND e.creation_date >= GREATEST(b.start_date::timestamp, :startDateTime)
              AND e.creation_date < LEAST(b.end_date::timestamp, :endDateTime) + INTERVAL '1 day'
              AND e.category_id = b.category_id
            WHERE b.amount > 0
            GROUP BY b.category_id, c.name, c.icon, c.color, b.amount
            ORDER BY b.amount DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("startDateTime", startDateTime);
        query.setParameter("endDateTime", endDateTime);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return results;
    }


    /**
     * Récupérer toutes les statistiques budgets en une seule requête optimisée
     * Utilisée par getAllStatistics() pour réduire les appels API
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return DTO unifié contenant toutes les statistiques budgets
     */
    public BudgetStatisticsDto getAllBudgetStatisticsUnified(Long userId, LocalDate startDate, LocalDate endDate) {
        BudgetStatisticsDto unified = new BudgetStatisticsDto();

        // Récupérer les données par catégorie (utilisé pour BudgetVsActual et Distribution)
        List<Object[]> categoryResults = getBudgetStatisticsData(userId, startDate, endDate);

        // Optimisé : un seul parcours pour créer BudgetVsActual et Distribution
        List<BudgetVsActualDto> budgetVsActual = new ArrayList<>();
        List<BudgetDistributionDto> distribution = new ArrayList<>();
        double totalBudgetAmount = 0.0;
        double totalSpentAmount = 0.0;

        // Parcourir une seule fois et créer les deux DTOs
        for (Object[] row : categoryResults) {
            Long categoryId = row[0] != null ? ((Number) row[0]).longValue() : null;
            String categoryName = (String) row[1];
            String icon = (String) row[2];
            String color = (String) row[3];
            Double budgetAmount = mapper.convertToDouble(row[4]);
            Double actualAmount = mapper.convertToDouble(row[5]);

            // Créer BudgetVsActualDto
            BudgetVsActualDto vsActualDto = new BudgetVsActualDto();
            vsActualDto.setCategoryId(categoryId);
            vsActualDto.setCategoryName(categoryName);
            vsActualDto.setIcon(icon);
            vsActualDto.setColor(color);
            vsActualDto.setBudgetAmount(budgetAmount);
            vsActualDto.setActualAmount(actualAmount);
            vsActualDto.setDifference(budgetAmount - actualAmount);
            vsActualDto.setPercentageUsed(budgetAmount > 0 ? (actualAmount / budgetAmount) * 100 : 0.0);
            budgetVsActual.add(vsActualDto);

            // Créer BudgetDistributionDto
            BudgetDistributionDto distributionDto = new BudgetDistributionDto();
            distributionDto.setCategoryId(categoryId);
            distributionDto.setCategoryName(categoryName);
            distributionDto.setIcon(icon);
            distributionDto.setColor(color);
            distributionDto.setBudgetAmount(budgetAmount);
            // Le pourcentage sera calculé après avoir le totalBudgetAmount
            distribution.add(distributionDto);

            // Accumuler les totaux
            totalBudgetAmount += budgetAmount;
            totalSpentAmount += actualAmount;
        }

        // Calculer les pourcentages pour Distribution maintenant qu'on a totalBudgetAmount
        for (BudgetDistributionDto dto : distribution) {
            dto.setPercentage(totalBudgetAmount > 0 ? (dto.getBudgetAmount() / totalBudgetAmount) * 100 : 0.0);
        }

        unified.setBudgetVsActual(budgetVsActual);
        unified.setDistribution(distribution);

        // Récupérer les données par budget individuel pour calculer efficiency
        // Optimisé : utilise creation_date directement (sans DATE()) et optimise GREATEST/LEAST
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        String budgetSql = """
            SELECT 
                b.id,
                b.amount,
                SUM(e.amount) as spent_amount
            FROM budgets b
            LEFT JOIN expenses e ON e.user_id = :userId
              AND e.deleted = false
              AND e.creation_date >= GREATEST(b.start_date::timestamp, :startDateTime)
              AND e.creation_date < LEAST(b.end_date::timestamp, :endDateTime) + INTERVAL '1 day'
              AND e.category_id = b.category_id
            WHERE b.user_id = :userId
              AND b.deleted = false
              AND b.start_date <= :endDate
              AND b.end_date >= :startDate
            GROUP BY b.id, b.amount
            ORDER BY b.id DESC
        """;

        Query budgetQuery = entityManager.createNativeQuery(budgetSql);
        budgetQuery.setParameter("userId", userId);
        budgetQuery.setParameter("startDateTime", startDateTime);
        budgetQuery.setParameter("endDateTime", endDateTime);
        budgetQuery.setParameter("startDate", startDate);
        budgetQuery.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> budgetResults = budgetQuery.getResultList();

        // Calculer budgets on track et exceeded
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

        // Créer BudgetEfficiencyDto
        BudgetEfficiencyDto efficiency = new BudgetEfficiencyDto();
        efficiency.setTotalBudgets(budgetResults.size());
        efficiency.setTotalBudgetAmount(totalBudgetAmount);
        efficiency.setTotalSpentAmount(totalSpentAmount);
        efficiency.setTotalRemainingAmount(totalBudgetAmount - totalSpentAmount);
        efficiency.setAveragePercentageUsed(totalBudgetAmount > 0 ? (totalSpentAmount / totalBudgetAmount) * 100 : 0.0);
        efficiency.setBudgetsOnTrack(budgetsOnTrack);
        efficiency.setBudgetsExceeded(budgetsExceeded);

        unified.setEfficiency(efficiency);

        return unified;
    }

    /**
     * Récupérer TOUTES les statistiques en une seule requête optimisée
     * Cette méthode unifie tous les endpoints pour réduire les appels API de 6 à 1
     * @param userId ID de l'utilisateur
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return DTO unifié contenant toutes les statistiques
     */
    public StatisticsDto getAllStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La date de début doit être antérieure ou égale à la date de fin");
        }

        StatisticsDto all = new StatisticsDto();

        // Charger toutes les données en utilisant les méthodes existantes
        all.setMonthlySummary(getPeriodSummary(userId, startDate, endDate));
        all.setCategoryExpenses(getExpensesByCategory(userId, startDate, endDate));
        all.setBudgetStatistics(getAllBudgetStatisticsUnified(userId, startDate, endDate));

        return all;
    }
}

