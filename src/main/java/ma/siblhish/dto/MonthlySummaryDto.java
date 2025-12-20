package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryDto {
    private String month; // Format: "2025-01"
    private Double totalIncome;
    private Double totalExpenses;
    private Double balance;
}

