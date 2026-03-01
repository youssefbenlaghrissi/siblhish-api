package ma.siblhish.repository;

import ma.siblhish.dto.BudgetDto;
import ma.siblhish.entities.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
     * Vérifie si un budget existe déjà pour cet utilisateur, catégorie et période (éviter doublons batch récurrent).
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Budget b
        WHERE b.user.id = :userId
        AND b.startDate = :startDate
        AND b.endDate = :endDate
        AND b.deleted = false
        AND (:categoryId IS NULL AND b.category IS NULL OR b.category.id = :categoryId)
        """)
    boolean existsBudgetForUserAndCategoryAndPeriod(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Trouve les budgets existants pour plusieurs utilisateurs et catégories en une seule requête.
     * Optimisé pour le batch processing dans RecurringBudgetScheduler.
     * 
     * @param userIds Liste des IDs utilisateurs
     * @param categoryIds Liste des IDs catégories
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Liste des budgets existants
     */
    @Query("""
        SELECT b FROM Budget b 
        WHERE b.user.id IN :userIds 
          AND b.category.id IN :categoryIds
          AND b.startDate = :startDate 
          AND b.endDate = :endDate 
          AND b.deleted = false
    """)
    List<Budget> findExistingBudgetsForMonth(
        @Param("userIds") List<Long> userIds,
        @Param("categoryIds") List<Long> categoryIds,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    //OK
    @Query("""
        SELECT b FROM Budget b 
        LEFT JOIN FETCH b.category 
        LEFT JOIN FETCH b.user
        WHERE b.user.id = :userId 
        AND b.deleted = false
        AND b.startDate <= :expenseDate 
        AND b.endDate >= :expenseDate
        AND (b.category IS NULL OR b.category.id = :categoryId)
    """)
    Optional<Budget> findCurrentBudgetByCategory(
        @Param("userId") Long userId,
        @Param("expenseDate") LocalDate expenseDate,
        @Param("categoryId") Long categoryId
    );

    /**
     * Récupérer les budgets d'un utilisateur avec le montant dépensé (spent)
     * Utilise la NamedQuery définie sur l'entité Budget.
     */
    //OK
    @Query(name = "Budget.findBudgetsWithSpentByUser")
    List<BudgetDto> findBudgetsByUser(@Param("userId") Long userId);

    /**
     * Récupérer les budgets d'un utilisateur avec le montant dépensé (spent)
     * Variante AVEC filtre de mois.
     */
    //OK
    @Query(name = "Budget.findBudgetsWithSpentByUserAndMonth")
    List<BudgetDto> findBudgetsByUserAndMonth(
            @Param("userId") Long userId,
            @Param("firstDayOfMonth") LocalDate firstDayOfMonth,
            @Param("lastDayOfMonth") LocalDate lastDayOfMonth
    );
    
}

