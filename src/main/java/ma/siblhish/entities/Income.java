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

/**
 * Income représente une transaction réelle d'entrée d'argent (revenu).
 * Contrairement à Budget (qui représente une limite de dépenses),
 * Income représente une transaction historique avec une date précise.
 * 
 * Exemples :
 * - Salaire reçu le 1er janvier 2024 : 8000 MAD
 * - Vente freelance le 15 janvier 2024 : 2000 MAD
 * - Prime reçue le 20 janvier 2024 : 1000 MAD
 */
@Entity
@Table(name = "incomes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Income extends AbstractEntity {

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

    @Column(name = "source")
    private String source; // Ex: Salaire, Freelance, Vente, etc.

    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency")
    private RecurrenceFrequency recurrenceFrequency;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

