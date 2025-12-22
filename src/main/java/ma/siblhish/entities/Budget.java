package ma.siblhish.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Budget représente une limite de dépenses prévue pour une période donnée.
 * Contrairement à Income (qui représente des transactions réelles d'entrée d'argent),
 * Budget représente une règle/plafond de dépenses.
 * 
 * Exemples :
 * - Budget mensuel global : 5000 MAD
 * - Budget mensuel pour "Alimentation" : 2000 MAD
 * - Budget hebdomadaire pour "Loisirs" : 500 MAD
 */
@Entity
@Table(name = "budgets")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Budget extends AbstractEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Double amount;

    /**
     * Catégorie associée. Si null, c'est un budget global (toutes catégories)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Date de début du budget (optionnel, pour budgets avec dates spécifiques)
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * Date de fin du budget (optionnel, pour budgets avec dates spécifiques)
     */
    @Column(name = "end_date")
    private LocalDate endDate;
}
