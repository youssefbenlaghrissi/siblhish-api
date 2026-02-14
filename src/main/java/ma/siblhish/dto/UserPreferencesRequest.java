package ma.siblhish.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour mettre à jour uniquement les préférences utilisateur
 * (notificationsEnabled et language)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesRequest {
    private Boolean notificationsEnabled;
    
    @Pattern(regexp = "^(fr|en|ar)$", message = "La langue doit être 'fr', 'en' ou 'ar'")
    private String language;
}

