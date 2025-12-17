package ma.siblhish.controller;

import ma.siblhish.dto.ApiResponse;
import ma.siblhish.dto.SocialLoginRequest;
import ma.siblhish.entities.User;
import ma.siblhish.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/social")
    public ResponseEntity<ApiResponse<User>> socialLogin(@RequestBody SocialLoginRequest request) {
        // Chercher ou créer l'utilisateur
        User user = userService.findOrCreateByEmail(
                request.getEmail(),
                request.getDisplayName(),
                request.getProvider()
        );

        return ResponseEntity.ok(ApiResponse.success(user));
    }
}