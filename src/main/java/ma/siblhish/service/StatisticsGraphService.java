package ma.siblhish.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import ma.siblhish.dto.CategoryExpenseDto;
import ma.siblhish.dto.FavoriteDto;
import ma.siblhish.dto.PeriodSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsGraphService {

    private final EntityManager entityManager;
    private final FavoriteService favoriteService;

    /**
     * Obtenir le résumé par période (revenus vs dépenses)
     * @param period : "day" (jour), "month" (mois), "year" (année)
     * - day : 30 derniers jours
     * - month : 12 derniers mois
     * - year : toutes les années trouvées
     */
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
                COALESCE(SUM(total_expenses), 0) as total_expenses
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
            dto.setTotalIncome(convertToDouble(row[1]));
            dto.setTotalExpenses(convertToDouble(row[2]));
            summaries.add(dto);
        }

        return summaries;
    }

    /**
     * Convertir une valeur numérique (BigDecimal ou Double) en double
     */
    private double convertToDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Obtenir les dépenses par catégorie
     */
    public List<CategoryExpenseDto> getExpensesByCategory(Long userId, String period) {
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
                COALESCE(SUM(e.amount), 0) as total_amount,
                COUNT(e.id) as transaction_count
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
                .mapToDouble(row -> convertToDouble(row[4]))
                .sum();

        List<CategoryExpenseDto> categoryExpenses = new ArrayList<>();
        for (Object[] row : results) {
            double amount = convertToDouble(row[4]);
            CategoryExpenseDto dto = new CategoryExpenseDto();
            dto.setCategoryId(((Number) row[0]).longValue());
            dto.setCategoryName((String) row[1]);
            dto.setAmount(amount);
            dto.setPercentage(totalAmount > 0 ? (amount / totalAmount) * 100 : 0);
            dto.setIcon((String) row[2]);
            dto.setColor((String) row[3]);
            categoryExpenses.add(dto);
        }

        return categoryExpenses;
    }

    /**
     * Obtenir les préférences des cartes statistiques
     * Retourne la liste des IDs de cartes favorisées (type="CARD")
     */
    public List<String> getCardsPreferences(Long userId) {
        List<FavoriteDto> favorites = favoriteService.getFavoritesByType(userId, "CARD");
        return favorites.stream()
                .map(f -> String.valueOf(f.getTargetEntity()))
                .collect(Collectors.toList());
    }

    /**
     * Mettre à jour les préférences des cartes statistiques
     * Supprime les anciens favoris de type "CARD" et ajoute les nouveaux
     */
    public List<String> updateCardsPreferences(Long userId, List<String> cards) {
        // Supprimer les anciens favoris de type "CARD"
        List<FavoriteDto> existingFavorites = favoriteService.getFavoritesByType(userId, "CARD");
        if (!existingFavorites.isEmpty()) {
            favoriteService.deleteFavorites(userId, existingFavorites);
        }
        
        // Ajouter les nouveaux favoris
        List<FavoriteDto> newFavorites = cards.stream()
                .map(cardId -> {
                    FavoriteDto dto = new FavoriteDto();
                    dto.setType("CARD");
                    dto.setTargetEntity(Long.parseLong(cardId));
                    dto.setValue("position=" + cards.indexOf(cardId)); // Position dans la liste
                    return dto;
                })
                .collect(Collectors.toList());
        
        favoriteService.addFavorites(userId, newFavorites);
        return cards;
    }
}

