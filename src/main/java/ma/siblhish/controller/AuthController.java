package ma.siblhish.controller;

import ma.siblhish.dto.ApiResponse;
import ma.siblhish.dto.SocialLoginRequest;
import ma.siblhish.dto.UserProfileDto;
import ma.siblhish.entities.User;
import ma.siblhish.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * POST /api/v1/auth/social
     * Authentification avec Google - Retourne un DTO simple sans relations
     */
    @PostMapping("/social")
    public ResponseEntity<ApiResponse<UserProfileDto>> socialLogin(@RequestBody SocialLoginRequest request) {
        try {
            User user = userService.findOrCreateByEmail(
                request.getEmail(),
                request.getDisplayName(),
                request.getProvider()
            );

            // Créer un DTO simple (SANS relations pour éviter JSON circulaire)
            UserProfileDto dto = new UserProfileDto();
            dto.setId(user.getId());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setType(user.getType());
            dto.setLanguage(user.getLanguage());
            dto.setMonthlySalary(user.getMonthlySalary());
            
            return ResponseEntity.ok(ApiResponse.success(dto, "Login successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }
}
