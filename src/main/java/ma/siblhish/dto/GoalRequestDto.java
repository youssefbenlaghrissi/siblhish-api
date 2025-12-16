package ma.siblhish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalRequestDto {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be positive")
    private Double targetAmount;

    private LocalDate targetDate;
    private Long categoryId; // Optional
}

