package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.siblhish.dto.ApiResponse;
import ma.siblhish.dto.LoginDto;
import ma.siblhish.dto.RegisterDto;
import ma.siblhish.dto.SocialLoginRequest;
import ma.siblhish.dto.UserProfileDto;
import ma.siblhish.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Authentification sociale (Google, Facebook, etc.)
     * Retourne le profil utilisateur
     */
    @PostMapping("/social")
    public ResponseEntity<ApiResponse<UserProfileDto>> socialLogin(
            @Valid @RequestBody SocialLoginRequest request) {
        UserProfileDto profile = userService.socialLogin(
                request.getEmail(),
                request.getDisplayName(),
                request.getProvider(),
                request.getNotificationsEnabled()
        );
        return ResponseEntity.ok(ApiResponse.success(profile, "Login successful"));
    }

    /**
     * Création de compte (inscription)
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileDto>> register(
            @Valid @RequestBody RegisterDto request) {
        try {
            log.info("🔐 Tentative de création de compte - Email: {}, NotificationsEnabled: {}", 
                request.getEmail(), request.getNotificationsEnabled());
            
            UserProfileDto profile = userService.register(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword(),
                request.getLanguage() != null ? request.getLanguage() : "fr",
                request.getNotificationsEnabled()
            );
            
            log.info("✅ Compte créé avec succès - User ID: {}, NotificationsEnabled: {}", 
                profile.getId(), profile.getNotificationsEnabled());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(profile, "Compte créé avec succès"));
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Erreur de validation lors de la création de compte: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de compte: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la création du compte: " + e.getMessage()));
        }
    }

    /**
     * Connexion avec email et mot de passe
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserProfileDto>> login(
            @Valid @RequestBody LoginDto request) {
        try {
            log.info("🔐 Tentative de connexion - Email: {}", request.getEmail());
            
            UserProfileDto profile = userService.login(
                request.getEmail(),
                request.getPassword()
            );
            
            log.info("✅ Connexion réussie - User ID: {}", profile.getId());
            
            return ResponseEntity.ok(ApiResponse.success(profile, "Connexion réussie"));
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Erreur d'authentification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la connexion: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Erreur lors de la connexion: " + e.getMessage()));
        }
    }
}
