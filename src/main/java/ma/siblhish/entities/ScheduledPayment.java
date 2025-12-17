package ma.siblhish.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.siblhish.enums.NotificationOption;
import ma.siblhish.enums.PaymentMethod;
import ma.siblhish.enums.RecurrenceFrequency;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_payments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduledPayment extends AbstractEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Double amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "beneficiary")
    private String beneficiary;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency")
    private RecurrenceFrequency recurrenceFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_option")
    private NotificationOption notificationOption = NotificationOption.NONE;

    @Column(name = "is_paid")
    private Boolean isPaid = false;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}

