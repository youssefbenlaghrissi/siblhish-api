package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodSummaryDto {
    private String period; // Format: "YYYY-MM-DD" (un point par jour du mois)
    private Double totalIncome;
    private Double totalExpenses;
    private Double balance;
}

