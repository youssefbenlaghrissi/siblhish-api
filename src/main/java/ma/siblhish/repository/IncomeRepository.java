package ma.siblhish.repository;

import ma.siblhish.entities.Income;
import ma.siblhish.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {

    @Query("SELECT i FROM Income i WHERE i.isRecurring = true AND i.deleted = false ORDER BY i.id DESC")
    List<Income> findByIsRecurringTrueOrderByIdDesc();
    
    @Query("SELECT i FROM Income i WHERE i.user.id = :userId AND i.deleted = false ORDER BY i.id DESC")
    List<Income> findByUserIdOrderByIdDesc(@Param("userId") Long userId);
    
    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.id = :userId AND i.deleted = false")
    Double getTotalIncomeByUserId(@Param("userId") Long userId);

    /**
     * Vérifie si un revenu similaire existe déjà pour la date (éviter doublons batch récurrent).
     * Utilisée pour des vérifications ponctuelles. Pour le batch, préférer une requête agrégée.
     */
    @Query("""
        SELECT COUNT(i) > 0 FROM Income i
        WHERE i.user.id = :userId
        AND i.amount = :amount
        AND FUNCTION('DATE', i.creationDate) = :creationDate
        AND i.deleted = false
        """)
    boolean existsSimilarRecurringIncome(
            @Param("userId") Long userId,
            @Param("amount") Double amount,
            @Param("creationDate") LocalDate creationDate);

    /**
     * Une seule requête pour récupérer les combinaisons (user, amount) qui ont déjà
     * un revenu à la date donnée. Optimisée pour le batch récurrent.
     * Résultat: [userId, amount]
     */
    @Query("""
        SELECT i.user.id, i.amount
        FROM Income i
        WHERE i.deleted = false
        AND FUNCTION('DATE', i.creationDate) = :creationDate
    """)
    List<Object[]> findRecurringIncomeKeysForDate(@Param("creationDate") LocalDate creationDate);
}

