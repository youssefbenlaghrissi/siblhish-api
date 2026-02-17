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
@NamedQueries({
        @NamedQuery(
                name = "Budget.findBudgetsWithSpentByUser",
                query = """
                    SELECT new ma.siblhish.dto.BudgetDto(
                        b.id,
                        b.user.id,
                        b.amount,
                        b.startDate,
                        b.endDate,
                        new ma.siblhish.dto.CategoryDto(c.id, c.name, c.icon, c.color),
                        COALESCE(SUM(e.amount), 0),
                        (b.amount - COALESCE(SUM(e.amount), 0)),
                        CASE 
                            WHEN b.amount > 0 THEN (COALESCE(SUM(e.amount), 0) * 100.0 / b.amount)
                            ELSE 0
                        END,
                        b.isRecurring
                    )
                    FROM Budget b
                    LEFT JOIN b.category c
                    LEFT JOIN Expense e ON e.user = b.user
                        AND e.deleted = false
                        AND e.creationDate BETWEEN b.startDate AND b.endDate
                        AND (b.category IS NULL OR e.category = b.category)
                    WHERE b.user.id = :userId
                      AND b.deleted = false
                    GROUP BY 
                        b.id, b.user.id, b.amount, b.startDate, b.endDate, b.isRecurring,
                        c.id, c.name, c.icon, c.color
                    ORDER BY b.amount DESC
                """
        ),
        @NamedQuery(
                name = "Budget.findBudgetsWithSpentByUserAndMonth",
                query = """
                    SELECT new ma.siblhish.dto.BudgetDto(
                        b.id,
                        b.user.id,
                        b.amount,
                        b.startDate,
                        b.endDate,
                        new ma.siblhish.dto.CategoryDto(c.id, c.name, c.icon, c.color),
                        COALESCE(SUM(e.amount), 0),
                        (b.amount - COALESCE(SUM(e.amount), 0)),
                        CASE 
                            WHEN b.amount > 0 THEN (COALESCE(SUM(e.amount), 0) * 100.0 / b.amount)
                            ELSE 0
                        END,
                        b.isRecurring
                    )
                    FROM Budget b
                    LEFT JOIN b.category c
                    LEFT JOIN Expense e ON e.user = b.user
                        AND e.deleted = false
                        AND e.creationDate BETWEEN b.startDate AND b.endDate
                        AND (b.category IS NULL OR e.category = b.category)
                    WHERE b.user.id = :userId
                      AND b.deleted = false
                      AND b.startDate <= :lastDayOfMonth
                      AND b.endDate >= :firstDayOfMonth
                    GROUP BY 
                        b.id, b.user.id, b.amount, b.startDate, b.endDate, b.isRecurring,
                        c.id, c.name, c.icon, c.color
                    ORDER BY b.amount DESC
                """
        )
})
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

    /**
     * Indique si le budget est récurrent (créé automatiquement chaque mois)
     */
    @Column(name = "is_recurring", nullable = false)
    private Boolean isRecurring = false;
}
