package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller pour la gestion du profil utilisateur
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Obtenir le profil utilisateur
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(@PathVariable Long userId) {
        UserProfileDto profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * Enregistrer ou mettre à jour le token FCM d'un utilisateur
     * 
     * Endpoint: POST /api/v1/users/{userId}/fcm-token
     * 
     * À quoi ça sert ?
     * - Enregistrer le token FCM de l'utilisateur dans la base de données
     * - Permettre au backend d'envoyer des notifications push à cet utilisateur
     * - Mettre à jour le token si l'utilisateur se connecte depuis un autre appareil
     */
    @PostMapping("/{userId}/fcm-token")
    public ResponseEntity<ApiResponse<Object>> registerFcmToken(
            @PathVariable Long userId,
            @Valid @RequestBody FcmTokenRequest request) {
        
        try {
            // Valider le token
            if (request.getFcmToken() == null || request.getFcmToken().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Le token FCM est requis"));
            }

            // Mettre à jour le token FCM dans la base de données
            userService.updateFcmToken(userId, request.getFcmToken());

            // Réponse de succès
            return ResponseEntity.ok(ApiResponse.success(
                    null,
                    "Token FCM enregistré avec succès"
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de l'enregistrement du token: " + e.getMessage()));
        }
    }

    /**
     * Mettre à jour uniquement les préférences utilisateur
     * (notificationsEnabled et language)
     * 
     * Endpoint: PATCH /api/v1/users/{userId}/preferences
     * 
     * À quoi ça sert ?
     * - Permettre à l'utilisateur de modifier uniquement ses préférences
     * - Ne permet PAS de modifier firstName, lastName ou email (sécurité)
     * - Les deux champs sont optionnels (on peut modifier l'un ou l'autre)
     * 
     * @param userId ID de l'utilisateur
     * @param request DTO contenant notificationsEnabled et language (optionnels)
     * @return UserProfileDto mis à jour
     */
    @PatchMapping("/{userId}/preferences")
    public ResponseEntity<ApiResponse<UserProfileDto>> updatePreferences(
            @PathVariable Long userId,
            @Valid @RequestBody UserPreferencesRequest request) {
        
        try {
            UserProfileDto updatedProfile = userService.updatePreferences(
                userId, 
                request.getNotificationsEnabled(), 
                request.getLanguage()
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                updatedProfile, 
                "Préférences mises à jour avec succès"
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors de la mise à jour des préférences: " + e.getMessage()));
        }
    }

}

