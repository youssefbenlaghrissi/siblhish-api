package ma.siblhish.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class ExpenseRequestDto {
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
    private String location;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private Boolean isRecurring = false;
    private RecurrenceFrequency recurrenceFrequency;
    private LocalDateTime recurrenceEndDate;
    private List<Integer> recurrenceDaysOfWeek; // 1=Monday, 7=Sunday
    private Integer recurrenceDayOfMonth; // 1-31
    private Integer recurrenceDayOfYear; // 1-365
}

