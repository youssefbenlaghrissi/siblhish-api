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
}
