package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthDataDto {
    private String month; // Format: "YYYY-MM"
    private Double income;
    private Double expenses;
    private Double balance;
}

