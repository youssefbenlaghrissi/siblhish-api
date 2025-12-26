package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour Efficacité Budgétaire
 * Mesure globale de l'efficacité des budgets
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetEfficiencyDto {
    private Double totalBudgetAmount; // Total des budgets
    private Double totalSpentAmount; // Total dépensé
    private Double totalRemainingAmount; // Total restant
    private Double averagePercentageUsed; // Pourcentage moyen utilisé
    private Integer totalBudgets; // Nombre total de budgets
    private Integer budgetsOnTrack; // Nombre de budgets respectés (< 100%)
    private Integer budgetsExceeded; // Nombre de budgets dépassés (> 100%)
}

