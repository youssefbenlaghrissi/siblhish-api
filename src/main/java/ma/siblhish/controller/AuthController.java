package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.ApiResponse;
import ma.siblhish.dto.SocialLoginRequest;
import ma.siblhish.dto.UserProfileDto;
import ma.siblhish.service.UserService;
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
                request.getProvider()
        );
        return ResponseEntity.ok(ApiResponse.success(profile, "Login successful"));
    }
}
