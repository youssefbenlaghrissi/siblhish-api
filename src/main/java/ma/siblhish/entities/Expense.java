package ma.siblhish.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.siblhish.enums.PaymentMethod;
import ma.siblhish.enums.RecurrenceFrequency;

import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Expense extends AbstractEntity {

    @NotNull
    @Positive
    @Column(nullable = false)
    private Double amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod method;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime date;

    @Column(name = "description")
    private String description;

    @Column(name = "location")
    private String location;

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency")
    private RecurrenceFrequency recurrenceFrequency;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
