package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodSummaryDto {
    private String period; // Format: "2025-01-15" (jour), "2025-01" (mois), "2025" (année)
    private Double totalIncome;
    private Double totalExpenses;
}

