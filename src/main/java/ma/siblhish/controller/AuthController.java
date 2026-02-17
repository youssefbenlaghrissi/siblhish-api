package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.ApiResponse;
import ma.siblhish.dto.LoginDto;
import ma.siblhish.dto.RegisterDto;
import ma.siblhish.dto.SocialLoginRequest;
import ma.siblhish.dto.UserProfileDto;
import ma.siblhish.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        UserProfileDto profile = userService.register(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword(),
                request.getLanguage() != null ? request.getLanguage() : "fr",
                request.getNotificationsEnabled()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(profile, "Compte créé avec succès"));
    }

    /**
     * Connexion avec email et mot de passe
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserProfileDto>> login(
            @Valid @RequestBody LoginDto request) {
        UserProfileDto profile = userService.login(
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.ok(ApiResponse.success(profile, "Connexion réussie"));
    }
}
