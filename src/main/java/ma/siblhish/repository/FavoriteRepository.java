package ma.siblhish.repository;

import ma.siblhish.entities.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    /**
     * Trouver tous les favoris d'un utilisateur par type
     */
    List<Favorite> findByUserIdAndTypeOrderById(Long userId, String type);
    
    /**
     * Récupérer tous les favoris existants pour un utilisateur et une liste de (type, targetEntity).
     * Optimisé pour éviter le problème N+1 lors de l'ajout/suppression en batch.
     * 
     * Note: Cette requête retourne tous les favoris qui matchent les types ET targetEntities fournis.
     * Le filtrage exact des tuples (type, targetEntity) est fait en mémoire pour plus de flexibilité.
     * 
     * @param userId ID de l'utilisateur
     * @param types Liste des types à rechercher
     * @param targetEntities Liste des targetEntity à rechercher
     * @return Liste des favoris correspondants
     */
    @Query(value = """
        SELECT f.* FROM favoris f
        WHERE f.user_id = :userId
          AND f.type IN :types
          AND f.target_entity IN :targetEntities
    """, nativeQuery = true)
    List<Favorite> findByUserIdAndTypeInAndTargetEntityIn(
            @Param("userId") Long userId,
            @Param("types") List<String> types,
            @Param("targetEntities") List<Long> targetEntities
    );
}

