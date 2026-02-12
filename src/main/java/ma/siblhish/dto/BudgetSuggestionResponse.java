package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour la réponse de suggestion de budgets
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetSuggestionResponse {
    
    private Double monthlyIncome;
    private String situation;
    private String location;
    private Double totalSuggestedBudget;
    private Double suggestedSavings;
    private List<BudgetSuggestion> budgets;
}

