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
     * Trouver un favori spécifique par userId, type et targetEntity
     */
    Optional<Favorite> findByUserIdAndTypeAndTargetEntity(Long userId, String type, Long targetEntity);

}

