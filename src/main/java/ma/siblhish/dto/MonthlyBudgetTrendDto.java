package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour Tendance Mensuelle Budgets
 * Évolution des budgets sur plusieurs mois
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyBudgetTrendDto {
    private String month; // Format: "2025-01"
    private Double totalBudgetAmount; // Total des budgets pour ce mois
    private Double totalSpentAmount; // Total dépensé pour ce mois
    private Double averagePercentageUsed; // Pourcentage moyen utilisé
    private Integer budgetCount; // Nombre de budgets actifs ce mois
}

