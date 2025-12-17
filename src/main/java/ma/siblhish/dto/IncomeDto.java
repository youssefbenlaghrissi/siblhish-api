package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.siblhish.enums.PaymentMethod;
import ma.siblhish.enums.RecurrenceFrequency;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomeDto {
    private Long id;
    private Double amount;
    private PaymentMethod method;
    private LocalDateTime date;
    private String description;
    private String source;
    private Boolean isRecurring;
    private RecurrenceFrequency recurrenceFrequency;
    private LocalDateTime recurrenceEndDate;
    private List<Integer> recurrenceDaysOfWeek;
    private Integer recurrenceDayOfMonth;
    private Integer recurrenceDayOfYear;
    private Long userId;
}

