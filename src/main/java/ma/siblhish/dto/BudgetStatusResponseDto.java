package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetStatusResponseDto {
    private Long budgetId;
    private Double amount;
    private Double spent;
    private Double remaining;
    private Double percentageUsed;
    private String status; // "OK", "WARNING", "EXCEEDED"
    private String message;
}

