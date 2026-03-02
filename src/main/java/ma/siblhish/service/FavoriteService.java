package ma.siblhish.service;

import jakarta.transaction.Transactional;
import ma.siblhish.dto.FavoriteDto;
import ma.siblhish.entities.Favorite;
import ma.siblhish.entities.User;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.FavoriteRepository;
import ma.siblhish.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final EntityMapper mapper;

    /**
     * Trouver tous les favoris d'un utilisateur par type
     * Utilisé pour les écrans : statistiques (type="CARD") et profil (type="CATEGORY_COLOR")
     */
    public List<FavoriteDto> getFavoritesByType(Long userId, String type) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Favorite> favorites = favoriteRepository.findByUserIdAndTypeOrderById(userId, type);
        return mapper.toFavoriteDtoList(favorites);
    }

    /**
     * Ajouter des favoris sélectionnés
     * Pour les cartes statistiques : type="CARD", targetEntity=ID de la carte, value="position=X"
     * Pour les couleurs de catégories : type="CATEGORY_COLOR", targetEntity=ID de la catégorie, value="#FF0000"
     * 
     * Optimisé : récupère tous les favoris existants en une seule requête au lieu de N requêtes.
     * Évite saveAll si rien n'a changé.
     */
    @Transactional
    public List<FavoriteDto> addFavorites(Long userId, List<FavoriteDto> favoritesToAdd) {
        if (favoritesToAdd == null || favoritesToAdd.isEmpty()) {
            throw new IllegalArgumentException("La liste des favoris ne peut pas être vide");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Extraire les types et targetEntities (garder l'ordre pour la requête VALUES)
        List<String> types = favoritesToAdd.stream()
                .map(FavoriteDto::getType)
                .toList();
        
        List<Long> targetEntities = favoritesToAdd.stream()
                .map(FavoriteDto::getTargetEntity)
                .toList();

        // Récupérer tous les favoris existants en UNE SEULE requête
        // On récupère un sur-ensemble puis on filtre en mémoire pour les tuples exacts
        Set<String> uniqueTypes = new HashSet<>(types);
        Set<Long> uniqueTargetEntities = new HashSet<>(targetEntities);
        
        List<Favorite> existingFavorites = favoriteRepository
                .findByUserIdAndTypeInAndTargetEntityIn(userId, new ArrayList<>(uniqueTypes), new ArrayList<>(uniqueTargetEntities));

        // Créer une Map pour lookup rapide O(1) : clé = "type:targetEntity"
        Map<String, Favorite> existingMap = existingFavorites.stream()
                .collect(Collectors.toMap(
                        f -> f.getType() + ":" + f.getTargetEntity(),
                        Function.identity(),
                        (existing, replacement) -> existing // En cas de doublon, garder le premier
                ));

        // Traiter chaque favori à ajouter et détecter les changements
        List<Favorite> favoritesToSave = new ArrayList<>();
        boolean hasChanges = false;

        for (FavoriteDto dto : favoritesToAdd) {
            String key = dto.getType() + ":" + dto.getTargetEntity();
            Favorite existing = existingMap.get(key);

            if (existing != null) {
                // Vérifier si la valeur a vraiment changé avant de marquer pour sauvegarde
                if (!Objects.equals(existing.getValue(), dto.getValue())) {
                    existing.setValue(dto.getValue());
                    favoritesToSave.add(existing);
                    hasChanges = true;
                }
                // Si pas de changement, on ne l'ajoute pas à favoritesToSave
            } else {
                // Créer un nouveau favori
                Favorite favorite = new Favorite();
                favorite.setUser(user);
                favorite.setType(dto.getType());
                favorite.setTargetEntity(dto.getTargetEntity());
                favorite.setValue(dto.getValue());
                favoritesToSave.add(favorite);
                hasChanges = true;
            }
        }

        // Sauvegarder uniquement si quelque chose a changé
        if (!hasChanges) {
            // Rien n'a changé, retourner les favoris existants
            return mapper.toFavoriteDtoList(existingFavorites);
        }

        List<Favorite> saved = favoriteRepository.saveAll(favoritesToSave);
        return mapper.toFavoriteDtoList(saved);
    }

    /**
     * Supprimer des favoris sélectionnés
     * Supprime les favoris correspondant aux type et targetEntity fournis
     * 
     * Optimisé : récupère tous les favoris à supprimer en une seule requête au lieu de N requêtes.
     */
    @Transactional
    public void deleteFavorites(Long userId, List<FavoriteDto> favoritesToDelete) {
        if (favoritesToDelete == null || favoritesToDelete.isEmpty()) {
            throw new IllegalArgumentException("La liste des favoris à supprimer ne peut pas être vide");
        }

        // Pas besoin de vérifier l'utilisateur : si l'ID n'existe pas, la requête retournera simplement une liste vide

        // Extraire les types et targetEntities (garder l'ordre pour la requête VALUES)
        List<String> types = favoritesToDelete.stream()
                .map(FavoriteDto::getType)
                .toList();
        
        List<Long> targetEntities = favoritesToDelete.stream()
                .map(FavoriteDto::getTargetEntity)
                .toList();

        // Récupérer tous les favoris existants en UNE SEULE requête
        // On récupère un sur-ensemble puis on filtre en mémoire pour les tuples exacts
        Set<String> uniqueTypes = new HashSet<>(types);
        Set<Long> uniqueTargetEntities = new HashSet<>(targetEntities);
        
        List<Favorite> allExisting = favoriteRepository
                .findByUserIdAndTypeInAndTargetEntityIn(userId, new ArrayList<>(uniqueTypes), new ArrayList<>(uniqueTargetEntities));

        // Créer une Map pour lookup rapide O(1) : clé = "type:targetEntity"
        Map<String, Favorite> existingMap = allExisting.stream()
                .collect(Collectors.toMap(
                        f -> f.getType() + ":" + f.getTargetEntity(),
                        Function.identity()
                ));

        // Filtrer uniquement les favoris qui existent et correspondent exactement aux DTOs fournis
        List<Favorite> favoritesToRemove = favoritesToDelete.stream()
                .map(dto -> {
                    String key = dto.getType() + ":" + dto.getTargetEntity();
                    return existingMap.get(key);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Supprimer tous les favoris trouvés en une seule opération
        if (!favoritesToRemove.isEmpty()) {
            favoriteRepository.deleteAll(favoritesToRemove);
        }
    }
}

