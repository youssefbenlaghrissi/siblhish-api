package ma.siblhish.repository;

import ma.siblhish.dto.BudgetDto;
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
    
    /**
     * Trouve les budgets actifs pour un utilisateur dans une période donnée.
     * Optimisé pour la vérification des budgets lors de la création d'une dépense.
     * 
     * @deprecated Utiliser findActiveBudgetsWithSpentForExpense à la place (optimisé avec calcul du spent)
     */
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
    List<Budget> findActiveBudgetsForExpense(
        @Param("userId") Long userId,
        @Param("expenseDate") LocalDate expenseDate,
        @Param("categoryId") Long categoryId
    );

    /**
     * Trouve les budgets actifs pour une dépense et calcule le montant dépensé (spent) en une seule requête.
     * Retourne SEULEMENT les budgets du mois concerné par la dépense.
     * Optimisé : Évite le problème N+1 queries en calculant le spent directement dans la requête.
     * 
     * @param userId ID de l'utilisateur
     * @param expenseDate Date de la dépense
     * @param categoryId ID de la catégorie de la dépense
     * @param firstDayOfMonth Premier jour du mois de la dépense (pour filtrer les budgets récurrents)
     * @param lastDayOfMonth Dernier jour du mois de la dépense (pour filtrer les budgets récurrents)
     * @return Liste de tableaux [budgetId, budgetAmount, startDate, endDate, categoryId, isRecurring, spent]
     */
    @Query("""
        SELECT 
            b.id,
            b.amount,
            b.startDate,
            b.endDate,
            b.category.id,
            b.isRecurring,
            COALESCE(SUM(e.amount), 0.0) as spent
        FROM Budget b
        LEFT JOIN Expense e ON e.user.id = b.user.id
            AND e.deleted = false
            AND e.creationDate >= b.startDate
            AND e.creationDate <= b.endDate
            AND (b.category IS NULL OR e.category.id = b.category.id)
        WHERE b.user.id = :userId
            AND b.deleted = false
            AND b.startDate <= :expenseDate
            AND b.endDate >= :expenseDate
            AND (b.category IS NULL OR b.category.id = :categoryId)
            -- Pour les budgets récurrents, retourner SEULEMENT celui du mois de la dépense
            -- Vérifier que le budget commence et finit dans le même mois que la dépense
            AND (
                b.isRecurring = false
                OR (b.isRecurring = true 
                    AND FUNCTION('YEAR', b.startDate) = FUNCTION('YEAR', :expenseDate)
                    AND FUNCTION('MONTH', b.startDate) = FUNCTION('MONTH', :expenseDate)
                    AND FUNCTION('YEAR', b.endDate) = FUNCTION('YEAR', :expenseDate)
                    AND FUNCTION('MONTH', b.endDate) = FUNCTION('MONTH', :expenseDate))
            )
        GROUP BY b.id, b.amount, b.startDate, b.endDate, b.category.id, b.isRecurring
        ORDER BY b.id
    """)
    List<Object[]> findActiveBudgetsWithSpentForExpense(
        @Param("userId") Long userId,
        @Param("expenseDate") LocalDate expenseDate,
        @Param("categoryId") Long categoryId,
        @Param("firstDayOfMonth") LocalDate firstDayOfMonth,
        @Param("lastDayOfMonth") LocalDate lastDayOfMonth
    );

    /**
     * Récupérer les budgets d'un utilisateur avec le montant dépensé (spent)
     * Utilise la NamedQuery définie sur l'entité Budget.
     */
    @Query(name = "Budget.findBudgetsWithSpentByUser")
    List<BudgetDto> findBudgetsWithSpentByUser(@Param("userId") Long userId);

    /**
     * Récupérer les budgets d'un utilisateur avec le montant dépensé (spent)
     * Variante AVEC filtre de mois.
     */
    @Query(name = "Budget.findBudgetsWithSpentByUserAndMonth")
    List<BudgetDto> findBudgetsWithSpentByUserAndMonth(
            @Param("userId") Long userId,
            @Param("firstDayOfMonth") LocalDate firstDayOfMonth,
            @Param("lastDayOfMonth") LocalDate lastDayOfMonth
    );
    
}

