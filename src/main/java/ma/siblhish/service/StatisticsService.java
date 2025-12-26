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
}

