package ma.siblhish.repository;

import ma.siblhish.entities.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
     * Supprimer tous les favoris d'un utilisateur par type
     */
    void deleteAllByUserIdAndType(Long userId, String type);

    /**
     * Supprimer un favori spécifique
     */
    void deleteByUserIdAndTypeAndTargetEntity(Long userId, String type, Long targetEntity);
    
    /**
     * Trouver un favori spécifique par userId, type et targetEntity
     */
    Optional<Favorite> findByUserIdAndTypeAndTargetEntity(Long userId, String type, Long targetEntity);
    
    /**
     * Trouver tous les favoris actifs d'un utilisateur, ordonnés par type puis par ID
     */
    @Query("SELECT f FROM Favorite f WHERE f.user.id = :userId ORDER BY f.type, f.id")
    List<Favorite> findByUserIdOrderByTypeAndId(Long userId);
}

