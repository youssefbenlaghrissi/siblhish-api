package ma.siblhish.repository;

import ma.siblhish.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Catégories utilisées par l'utilisateur (via ses dépenses)
    @Query("SELECT DISTINCT c FROM Category c JOIN c.expenses e WHERE e.user.id = :userId ORDER BY c.id DESC")
    List<Category> findCategoriesByUserId(@Param("userId") Long userId);
    
    // Toutes les catégories (pour les paramètres)
    @Query("SELECT c FROM Category c ORDER BY c.id DESC")
    List<Category> findAllCategories();
}

