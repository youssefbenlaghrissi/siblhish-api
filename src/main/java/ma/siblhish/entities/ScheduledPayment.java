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
import java.util.List;

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

    @Column(name = "recurrence_end_date")
    private LocalDateTime recurrenceEndDate;

    @ElementCollection
    @CollectionTable(name = "scheduled_payment_recurrence_days", joinColumns = @JoinColumn(name = "scheduled_payment_id"))
    @Column(name = "day_of_week")
    private List<Integer> recurrenceDaysOfWeek; // 1=Monday, 7=Sunday

    @Column(name = "recurrence_day_of_month")
    private Integer recurrenceDayOfMonth; // 1-31

    @Column(name = "recurrence_day_of_year")
    private Integer recurrenceDayOfYear; // 1-365

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

