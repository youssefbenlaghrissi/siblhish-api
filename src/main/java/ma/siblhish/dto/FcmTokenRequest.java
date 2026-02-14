package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête d'enregistrement du token FCM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenRequest {
    private String fcmToken;
}

