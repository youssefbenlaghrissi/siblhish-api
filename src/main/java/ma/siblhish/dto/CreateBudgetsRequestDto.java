package ma.siblhish.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour créer plusieurs budgets en une seule transaction
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBudgetsRequestDto {
    @NotEmpty(message = "La liste des budgets ne peut pas être vide")
    @Valid
    private List<BudgetRequestDto> budgets;
}

