package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.siblhish.enums.PaymentMethod;
import ma.siblhish.enums.RecurrenceFrequency;

import java.time.LocalDateTime;

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
    private Long userId;
}

