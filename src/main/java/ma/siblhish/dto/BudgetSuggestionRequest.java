package ma.siblhish.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO pour la requête de suggestion de budgets
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSuggestionRequest {
    
    @NotNull(message = "Le revenu mensuel est requis")
    @Positive(message = "Le revenu mensuel doit être positif")
    private Double monthlyIncome;
    
    @NotBlank(message = "La situation est requise")
    private String situation; // Célibataire, En couple, Famille, Étudiant
    
    @NotBlank(message = "La localisation est requise")
    private String location; // ville, campagne
    
    @NotEmpty(message = "Au moins une catégorie doit être sélectionnée")
    private List<Long> categoryIds;
}

