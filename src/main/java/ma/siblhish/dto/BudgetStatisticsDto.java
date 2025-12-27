package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO unifié pour toutes les statistiques budgets
 * Contient toutes les données nécessaires pour les 4 graphiques budgets :
 * - Budget vs Réel
 * - Top Catégories Budgétisées (dérivé de Budget vs Réel)
 * - Efficacité Budgétaire
 * - Répartition des Budgets
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetStatisticsDto {
    /**
     * Données pour le graphique Budget vs Réel
     * Utilisé aussi pour Top Catégories Budgétisées (même source de données)
     */
    private List<BudgetVsActualDto> budgetVsActual;
    
    /**
     * Données pour le graphique Efficacité Budgétaire
     */
    private BudgetEfficiencyDto efficiency;
    
    /**
     * Données pour le graphique Répartition des Budgets (pie chart)
     */
    private List<BudgetDistributionDto> distribution;
}

