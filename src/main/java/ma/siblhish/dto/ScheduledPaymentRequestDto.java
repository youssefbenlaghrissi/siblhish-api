package ma.siblhish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.siblhish.enums.NotificationOption;
import ma.siblhish.enums.PaymentMethod;
import ma.siblhish.enums.RecurrenceFrequency;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledPaymentRequestDto {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String beneficiary;

    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private Boolean isRecurring = false;
    private RecurrenceFrequency recurrenceFrequency;
    private LocalDateTime recurrenceEndDate;
    private List<Integer> recurrenceDaysOfWeek; // 1=Monday, 7=Sunday
    private Integer recurrenceDayOfMonth; // 1-31
    private Integer recurrenceDayOfYear; // 1-365
    private NotificationOption notificationOption = NotificationOption.NONE;
}

