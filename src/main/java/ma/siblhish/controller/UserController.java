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
     * Mettre à jour le profil
     */
    @PutMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UserProfileUpdateDto request) {
        UserProfileDto profile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * Mettre à jour le mot de passe
     */
    @PutMapping("/{userId}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody PasswordChangeDto request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password updated successfully"));
    }
}

