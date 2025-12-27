package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO unifié pour TOUTES les statistiques
 * Contient toutes les données nécessaires pour tous les graphiques :
 * - Monthly Summary (bar chart, savings, averages)
 * - Category Expenses (pie chart)
 * - Budget Statistics (tous les graphiques budgets)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsDto {
    /**
     * Données pour le graphique en barres (revenus vs dépenses par période)
     * Utilisé aussi pour : Savings, Average Expense, Average Income
     */
    private List<PeriodSummaryDto> monthlySummary;
    
    /**
     * Données pour le graphique pie chart (répartition par catégorie)
     */
    private CategoryExpensesDto categoryExpenses;
    
    /**
     * Données pour tous les graphiques budgets
     * Contient : Budget vs Réel, Top Catégories, Efficacité, Répartition
     */
    private BudgetStatisticsDto budgetStatistics;
}
