package ma.siblhish.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.siblhish.enums.PaymentMethod;
import ma.siblhish.enums.RecurrenceFrequency;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomeRequestDto {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    @NotNull(message = "Date is required")
    private LocalDateTime date;

    private String description;
    private String source;

    private Boolean isRecurring = false;
    private RecurrenceFrequency recurrenceFrequency;
}

