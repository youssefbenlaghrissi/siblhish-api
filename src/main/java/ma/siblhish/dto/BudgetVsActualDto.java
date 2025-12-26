package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour le graphique Budget vs Réel
 * Compare le budget prévu avec les dépenses réelles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetVsActualDto {
    private Long categoryId;
    private String categoryName;
    private String icon;
    private String color;
    private Double budgetAmount; // Montant budgété
    private Double actualAmount; // Montant réel dépensé
    private Double difference; // Différence (budget - réel)
    private Double percentageUsed; // Pourcentage utilisé (réel / budget * 100)
}

