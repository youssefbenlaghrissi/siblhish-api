package ma.siblhish.repository;

import ma.siblhish.entities.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e WHERE e.isRecurring = true AND e.deleted = false ORDER BY e.id DESC")
    List<Expense> findByIsRecurringTrueOrderByIdDesc();

    //OK
    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId AND e.deleted = false ORDER BY e.id DESC")
    List<Expense> findByUserIdOrderByIdDesc(@Param("userId") Long userId);

    //OK
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.deleted = false")
    Double getTotalExpensesByUserId(@Param("userId") Long userId);

    /**
     * Calcule le montant total dépensé pour un budget spécifique.
     */
    //OK
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0.0) 
        FROM Expense e 
        WHERE e.user.id = :userId 
        AND e.deleted = false
        AND e.creationDate >= :startDate 
        AND e.creationDate <= :endDate
        AND (:categoryId IS NULL OR e.category.id = :categoryId)
    """)
    Double calculateSpentForBudget(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("categoryId") Long categoryId);

    /**
     * Vérifie si une dépense similaire existe déjà pour la date (éviter doublons batch récurrent).
     * Utilisée pour des vérifications ponctuelles. Pour le batch, préférer une requête agrégée.
     */
    @Query("""
        SELECT COUNT(e) > 0 FROM Expense e
        WHERE e.user.id = :userId
        AND e.amount = :amount
        AND FUNCTION('DATE', e.creationDate) = :creationDate
        AND e.deleted = false
        AND (:categoryId IS NULL AND e.category IS NULL OR e.category.id = :categoryId)
        """)
    boolean existsSimilarRecurringExpense(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("amount") Double amount,
            @Param("creationDate") LocalDate creationDate);

    /**
     * Une seule requête pour récupérer les combinaisons (user, category, amount) qui ont déjà
     * une dépense à la date donnée. Optimisée pour le batch récurrent.
     * Résultat: [userId, categoryId, amount]
     */
    @Query("""
        SELECT e.user.id, e.category.id, e.amount
        FROM Expense e
        WHERE e.deleted = false
        AND FUNCTION('DATE', e.creationDate) = :creationDate
    """)
    List<Object[]> findRecurringExpenseKeysForDate(@Param("creationDate") LocalDate creationDate);

}

