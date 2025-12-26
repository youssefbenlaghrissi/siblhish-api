package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour Répartition des Budgets (Pie Chart)
 * Répartition du budget total par catégorie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDistributionDto {
    private Long categoryId;
    private String categoryName;
    private String icon;
    private String color;
    private Double budgetAmount; // Montant budgété pour cette catégorie
    private Double percentage; // Pourcentage du budget total
}

