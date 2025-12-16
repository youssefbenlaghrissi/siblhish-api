package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.siblhish.enums.PeriodFrequency;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDto {
    private Long id;
    private Long userId;
    private Double amount;
    private PeriodFrequency period;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private CategoryDto category;
    private Double spent; // Calculated field
    private Double remaining; // Calculated field
    private Double percentageUsed; // Calculated field
}

