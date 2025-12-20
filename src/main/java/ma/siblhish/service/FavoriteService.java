package ma.siblhish.service;

import jakarta.transaction.Transactional;
import ma.siblhish.dto.FavoriteDto;
import ma.siblhish.entities.Favorite;
import ma.siblhish.entities.User;
import ma.siblhish.repository.FavoriteRepository;
import ma.siblhish.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;

    /**
     * Convertir une entité Favorite en DTO
     */
    private FavoriteDto toDto(Favorite favorite) {
        return new FavoriteDto(
            favorite.getId(),
            favorite.getUser().getId(),
            favorite.getType(),
            favorite.getTargetEntity(),
            favorite.getValue()
        );
    }

    /**
     * Trouver tous les favoris d'un utilisateur par type
     * Utilisé pour les écrans : statistiques (type="CARD") et profil (type="CATEGORY_COLOR")
     */
    public List<FavoriteDto> getFavoritesByType(Long userId, String type) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Favorite> favorites = favoriteRepository.findByUserIdAndTypeOrderById(userId, type);
        return favorites.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Ajouter des favoris sélectionnés
     * Pour les cartes statistiques : type="CARD", targetEntity=ID de la carte, value="position=X"
     * Pour les couleurs de catégories : type="CATEGORY_COLOR", targetEntity=ID de la catégorie, value="#FF0000"
     */
    @Transactional
    public List<FavoriteDto> addFavorites(Long userId, List<FavoriteDto> favoritesToAdd) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Favorite> newFavorites = favoritesToAdd.stream()
                .map(dto -> {
                    // Vérifier si le favori existe déjà
                    Favorite existing = favoriteRepository
                            .findByUserIdAndTypeAndTargetEntity(userId, dto.getType(), dto.getTargetEntity())
                            .orElse(null);

                    if (existing != null) {
                        // Mettre à jour le favori existant
                        existing.setValue(dto.getValue());
                        return existing;
                    } else {
                        // Créer un nouveau favori
                        Favorite favorite = new Favorite();
                        favorite.setUser(user);
                        favorite.setType(dto.getType());
                        favorite.setTargetEntity(dto.getTargetEntity());
                        favorite.setValue(dto.getValue());
                        return favorite;
                    }
                })
                .collect(Collectors.toList());

        List<Favorite> saved = favoriteRepository.saveAll(newFavorites);
        return saved.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Supprimer des favoris sélectionnés
     * Supprime les favoris correspondant aux type et targetEntity fournis
     */
    @Transactional
    public void deleteFavorites(Long userId, List<FavoriteDto> favoritesToDelete) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Trouver tous les favoris à supprimer en une seule requête
        List<Favorite> favoritesToRemove = favoritesToDelete.stream()
                .map(dto -> favoriteRepository.findByUserIdAndTypeAndTargetEntity(
                        userId,
                        dto.getType(),
                        dto.getTargetEntity()
                ))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());

        // Supprimer tous les favoris trouvés en une seule opération
        favoriteRepository.deleteAll(favoritesToRemove);
    }
}

