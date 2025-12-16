package ma.siblhish.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "goals")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Goal extends AbstractEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Positive
    @Column(name = "target_amount", nullable = false)
    private Double targetAmount;

    @Column(name = "current_amount", nullable = false)
    private Double currentAmount = 0.0;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // Optionnel : objectif lié à une catégorie spécifique

    @Column(name = "is_achieved", nullable = false)
    private Boolean isAchieved = false;
}
