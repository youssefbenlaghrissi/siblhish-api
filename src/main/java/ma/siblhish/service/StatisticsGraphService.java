package ma.siblhish.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import ma.siblhish.dto.CategoryExpenseDto;
import ma.siblhish.dto.FavoriteDto;
import ma.siblhish.dto.MonthlySummaryDto;
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
     * Obtenir le résumé mensuel (revenus vs dépenses)
     */
    public List<MonthlySummaryDto> getMonthlySummary(Long userId, String year) {
        String sql = """
            SELECT 
                month,
                COALESCE(SUM(total_income), 0) as total_income,
                COALESCE(SUM(total_expenses), 0) as total_expenses,
                COALESCE(SUM(total_income), 0) - COALESCE(SUM(total_expenses), 0) as balance
            FROM (
                SELECT 
                    TO_CHAR(creation_date, 'YYYY-MM') as month,
                    amount as total_income,
                    0 as total_expenses
                FROM incomes
                WHERE user_id = :userId AND EXTRACT(YEAR FROM creation_date) = :year
                UNION ALL
                SELECT 
                    TO_CHAR(creation_date, 'YYYY-MM') as month,
                    0 as total_income,
                    amount as total_expenses
                FROM expenses
                WHERE user_id = :userId AND EXTRACT(YEAR FROM creation_date) = :year
            ) combined
            GROUP BY month
            ORDER BY month
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("year", Integer.parseInt(year));

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<MonthlySummaryDto> summaries = new ArrayList<>();
        for (Object[] row : results) {
            MonthlySummaryDto dto = new MonthlySummaryDto();
            dto.setMonth((String) row[0]);
            dto.setTotalIncome(((BigDecimal) row[1]).doubleValue());
            dto.setTotalExpenses(((BigDecimal) row[2]).doubleValue());
            dto.setBalance(((BigDecimal) row[3]).doubleValue());
            summaries.add(dto);
        }

        return summaries;
    }

    /**
     * Obtenir les dépenses par catégorie
     */
    public List<CategoryExpenseDto> getExpensesByCategory(Long userId, String period) {
        String dateCondition = switch (period) {
            case "week" -> "e.creation_date >= CURRENT_DATE - INTERVAL '7 days'";
            case "month" -> "EXTRACT(MONTH FROM e.creation_date) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM e.creation_date) = EXTRACT(YEAR FROM CURRENT_DATE)";
            case "quarter" -> "e.creation_date >= DATE_TRUNC('quarter', CURRENT_DATE)";
            case "year" -> "EXTRACT(YEAR FROM e.creation_date) = EXTRACT(YEAR FROM CURRENT_DATE)";
            default -> "EXTRACT(MONTH FROM e.creation_date) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM e.creation_date) = EXTRACT(YEAR FROM CURRENT_DATE)";
        };

        String sql = """
            SELECT 
                c.id as category_id,
                c.name as category_name,
                c.icon as category_icon,
                c.color as category_color,
                COALESCE(SUM(e.amount), 0) as total_amount,
                COUNT(e.id) as transaction_count
            FROM categories c
            LEFT JOIN expenses e ON c.id = e.category_id AND e.user_id = :userId AND """ + dateCondition + """
            WHERE c.user_id = :userId OR c.user_id IS NULL
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
                .mapToDouble(row -> ((BigDecimal) row[4]).doubleValue())
                .sum();

        List<CategoryExpenseDto> categoryExpenses = new ArrayList<>();
        for (Object[] row : results) {
            double amount = ((BigDecimal) row[4]).doubleValue();
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

