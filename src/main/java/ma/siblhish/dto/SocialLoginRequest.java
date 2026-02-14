package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequest {

    private String provider;    // "google" ou "facebook"
    private String idToken;
    private String email;
    private String displayName;
    private String photoUrl;
    
    /**
     * Statut des notifications activées ou non
     * true si l'utilisateur a accepté les permissions de notifications
     * false si l'utilisateur a refusé les permissions
     * null si les permissions n'ont pas encore été demandées
     */
    private Boolean notificationsEnabled;
}
