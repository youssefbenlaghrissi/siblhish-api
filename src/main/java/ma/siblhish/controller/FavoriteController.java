package ma.siblhish.controller;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.ApiResponse;
import ma.siblhish.dto.FavoriteDto;
import ma.siblhish.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * Trouver tous les favoris d'un utilisateur par type
     * Exemples de types : "CARD" (pour statistiques), "CATEGORY_COLOR" (pour profil)
     */
    @GetMapping("/{userId}/type/{type}")
    public ResponseEntity<ApiResponse<List<FavoriteDto>>> getFavoritesByType(
            @PathVariable Long userId,
            @PathVariable String type) {
        List<FavoriteDto> favorites = favoriteService.getFavoritesByType(userId, type);
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }

    /**
     * Ajouter des favoris sélectionnés
     * Body : Liste de FavoriteDto avec type, targetEntity et value
     */
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<FavoriteDto>>> addFavorites(
            @PathVariable Long userId,
            @RequestBody List<FavoriteDto> favorites) {
        if (favorites == null || favorites.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La liste des favoris ne peut pas être vide"));
        }
        List<FavoriteDto> added = favoriteService.addFavorites(userId, favorites);
        return ResponseEntity.ok(ApiResponse.success(added));
    }

    /**
     * Supprimer des favoris sélectionnés
     * Body : Liste de FavoriteDto avec type et targetEntity (value optionnel)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteFavorites(
            @PathVariable Long userId,
            @RequestBody List<FavoriteDto> favorites) {
        if (favorites == null || favorites.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La liste des favoris à supprimer ne peut pas être vide"));
        }
        favoriteService.deleteFavorites(userId, favorites);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

