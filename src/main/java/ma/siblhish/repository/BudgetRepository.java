package ma.siblhish.repository;

import ma.siblhish.entities.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    /**
     * Trouve tous les budgets récurrents.
     * Utilise une méthode de requête dérivée Spring Data JPA (plus performante que SQL natif).
     */
    @Query("SELECT b FROM Budget b WHERE b.isRecurring = true AND b.deleted = false ORDER BY b.id DESC")
    List<Budget> findByIsRecurringTrueOrderByIdDesc();
    
    /**
     * Trouve les budgets pour un utilisateur, une catégorie et une période donnée.
     * Utilise une méthode de requête dérivée Spring Data JPA.
     */
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.category.id = :categoryId " +
           "AND b.startDate = :startDate AND b.endDate = :endDate AND b.deleted = false ORDER BY b.id DESC")
    List<Budget> findByUserIdAndCategoryIdAndStartDateAndEndDateOrderByIdDesc(
        @Param("userId") Long userId,
        @Param("categoryId") Long categoryId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * Trouve les budgets globaux (sans catégorie) pour un utilisateur et une période donnée.
     * Utilise une méthode de requête dérivée Spring Data JPA.
     */
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.category IS NULL " +
           "AND b.startDate = :startDate AND b.endDate = :endDate AND b.deleted = false ORDER BY b.id DESC")
    List<Budget> findByUserIdAndCategoryIsNullAndStartDateAndEndDateOrderByIdDesc(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
}

