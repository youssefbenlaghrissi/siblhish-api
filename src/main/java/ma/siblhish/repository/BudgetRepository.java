package ma.siblhish.repository;

import ma.siblhish.entities.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    /**
     * Trouve tous les budgets récurrents.
     * Utilise une méthode de requête dérivée Spring Data JPA (plus performante que SQL natif).
     */
    List<Budget> findByIsRecurringTrueOrderByIdDesc();
    
    /**
     * Trouve les budgets pour un utilisateur, une catégorie et une période donnée.
     * Utilise une méthode de requête dérivée Spring Data JPA.
     */
    List<Budget> findByUserIdAndCategoryIdAndStartDateAndEndDateOrderByIdDesc(
        Long userId,
        Long categoryId,
        LocalDate startDate,
        LocalDate endDate
    );
    
    /**
     * Trouve les budgets globaux (sans catégorie) pour un utilisateur et une période donnée.
     * Utilise une méthode de requête dérivée Spring Data JPA.
     */
    List<Budget> findByUserIdAndCategoryIsNullAndStartDateAndEndDateOrderByIdDesc(
        Long userId,
        LocalDate startDate,
        LocalDate endDate
    );
    
}

