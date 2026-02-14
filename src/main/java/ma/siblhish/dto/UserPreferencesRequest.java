package ma.siblhish.dto;

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
    private String language;
}

