package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour une suggestion de budget individuelle
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSuggestion {
    
    private Long categoryId;
    private String categoryName;
    private Double amount;
    private Double percentage;
    private String icon;
    private String color;
}

