package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetStatusDto {
    private Double totalBudget;
    private Double spent;
    private Double remaining;
    private Double percentageUsed;
}

