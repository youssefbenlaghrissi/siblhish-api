package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.siblhish.enums.NotificationOption;
import ma.siblhish.enums.PaymentMethod;
import ma.siblhish.enums.RecurrenceFrequency;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledPaymentDto {
    private Long id;
    private String name;
    private Double amount;
    private PaymentMethod paymentMethod;
    private String beneficiary;
    private LocalDateTime dueDate;
    private Boolean isRecurring;
    private RecurrenceFrequency recurrenceFrequency;
    private NotificationOption notificationOption;
    private Boolean isPaid;
    private LocalDateTime paidDate;
    private Long userId;
    private Long categoryId;
    private CategoryDto category;
}

