package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalDto {
    private Long id;
    private String name;
    private String description;
    private Double targetAmount;
    private Double currentAmount;
    private Double progress; // Calculated: (currentAmount / targetAmount) * 100
    private LocalDate targetDate;
    private Boolean isAchieved;
    private LocalDateTime achievedDate; // Date et heure d'atteinte au format ISO 8601
    private Long userId;
    private CategoryDto category;
}

