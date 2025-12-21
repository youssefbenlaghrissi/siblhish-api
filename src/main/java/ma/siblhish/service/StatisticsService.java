package ma.siblhish.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.mapper.EntityMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    @PersistenceContext
    private EntityManager entityManager;
    private final EntityMapper mapper;

    /**
     * Obtenir les dépenses par catégorie selon la période
     * @param period : "day" (30 derniers jours), "month" (12 derniers mois), "year" (toutes les années)
     */
    public StatisticsDto getExpensesByCategory(Long userId, String period) {
        String periodLower = period != null ? period.toLowerCase() : "month";
        String dateCondition;
        
        switch (periodLower) {
            case "day":
                // 30 derniers jours
                dateCondition = "e.creation_date >= CURRENT_DATE - INTERVAL '30 days'";
                break;
            case "year":
                // Toutes les années trouvées (pas de filtre)
                dateCondition = "1=1";
                break;
            case "month":
            default:
                // 12 derniers mois
                dateCondition = "e.creation_date >= CURRENT_DATE - INTERVAL '12 months'";
                break;
        }

        String sql = """
            SELECT 
                c.id as category_id,
                c.name as category_name,
                c.icon as category_icon,
                c.color as category_color,
                COALESCE(SUM(e.amount), 0) as total_amount
            FROM categories c
            LEFT JOIN expenses e ON c.id = e.category_id AND e.user_id = :userId AND """ + " " + dateCondition + """
            GROUP BY c.id, c.name, c.icon, c.color
            HAVING COALESCE(SUM(e.amount), 0) > 0
            ORDER BY total_amount DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);

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

    public List<PeriodSummaryDto> getPeriodSummary(Long userId, String period) {
        // Déterminer le format de groupement selon la période
        String periodFormat;
        String dateFilter;

        switch (period != null ? period.toLowerCase() : "month") {
            case "day":
                // Grouper par jour (année-mois-jour) - 30 derniers jours
                periodFormat = "TO_CHAR(creation_date, 'YYYY-MM-DD')";
                dateFilter = "creation_date >= CURRENT_DATE - INTERVAL '30 days'";
                break;
            case "year":
                // Grouper par année - toutes les années trouvées (pas de filtre de date)
                periodFormat = "TO_CHAR(creation_date, 'YYYY')";
                dateFilter = "1=1"; // Pas de filtre, retourner toutes les années
                break;
            case "month":
            default:
                // Grouper par mois (année-mois) - 12 derniers mois
                periodFormat = "TO_CHAR(creation_date, 'YYYY-MM')";
                dateFilter = "creation_date >= CURRENT_DATE - INTERVAL '12 months'";
                break;
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
                WHERE user_id = :userId AND %s
                UNION ALL
                SELECT 
                    %s as period,
                    0 as total_income,
                    amount as total_expenses
                FROM expenses
                WHERE user_id = :userId AND %s
            ) combined
            GROUP BY period
            ORDER BY period
        """, periodFormat, dateFilter, periodFormat, dateFilter);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);

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

