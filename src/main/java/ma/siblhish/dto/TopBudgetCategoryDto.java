package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour Top Catégories Budgétisées
 * Liste les catégories avec les budgets les plus importants
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopBudgetCategoryDto {
    private Long categoryId;
    private String categoryName;
    private String icon;
    private String color;
    private Double budgetAmount; // Montant budgété
    private Double spentAmount; // Montant dépensé
    private Double remainingAmount; // Montant restant
    private Double percentageUsed; // Pourcentage utilisé
}

