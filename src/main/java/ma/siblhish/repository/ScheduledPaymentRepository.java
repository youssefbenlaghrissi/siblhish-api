package ma.siblhish.repository;

import ma.siblhish.entities.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {

    /**
     * Récupère les paiements planifiés avec toutes les relations chargées en une seule requête
     * Optimisation N+1 : utilise JOIN FETCH pour charger category et recurrenceDaysOfWeek
     */
    @Query("""
            SELECT DISTINCT sp
            FROM ScheduledPayment sp
            LEFT JOIN FETCH sp.category
            LEFT JOIN FETCH sp.recurrenceDaysOfWeek
            WHERE sp.user.id = :userId
                  and sp.deleted = false
            ORDER BY sp.id DESC
    """)
    List<ScheduledPayment> findByUserId(@Param("userId") Long userId);

    /**
     * Récupère les paiements non payés avec toutes les relations chargées en une seule requête
     * Optimisation N+1 : utilise JOIN FETCH pour charger category et recurrenceDaysOfWeek
     */
    @Query("SELECT DISTINCT sp FROM ScheduledPayment sp " +
           "LEFT JOIN FETCH sp.category " +
           "LEFT JOIN FETCH sp.recurrenceDaysOfWeek " +
           "WHERE sp.user.id = :userId AND sp.isPaid = false " +
           "ORDER BY sp.id DESC")
    List<ScheduledPayment> findUnpaidByUserId(@Param("userId") Long userId);

    /**
     * Trouve les paiements planifiés non payés qui nécessitent une notification.
     * Optimisé pour le scheduler de rappels.
     */
    @Query("""
        SELECT DISTINCT sp FROM ScheduledPayment sp
        LEFT JOIN FETCH sp.category
        LEFT JOIN FETCH sp.user
        WHERE (sp.isPaid = false OR sp.isPaid IS NULL)
        AND sp.dueDate IS NOT NULL
        AND sp.deleted = false
        AND (
            (sp.notificationOption IS NOT NULL AND sp.notificationOption != 'NONE')
            OR sp.dueDate < :today
        )
    """)
    List<ScheduledPayment> findPaymentsToNotify(@Param("today") LocalDateTime today);

    /**
     * Trouve les paiements récurrents qui nécessitent la création du prochain paiement.
     * Optimisé pour le scheduler de paiements récurrents.
     */
    @Query("""
        SELECT DISTINCT sp FROM ScheduledPayment sp
        LEFT JOIN FETCH sp.category
        LEFT JOIN FETCH sp.user
        WHERE sp.isRecurring = true
        AND sp.recurrenceFrequency IS NOT NULL
        AND sp.deleted = false
        AND (
            sp.isPaid = true
            OR (sp.dueDate IS NOT NULL AND sp.dueDate < :now)
        )
    """)
    List<ScheduledPayment> findRecurringPaymentsToProcess(@Param("now") LocalDateTime now);

    /**
     * Vérifie si un paiement similaire existe déjà pour éviter les doublons.
     * Optimisé : utilise COUNT() au lieu de charger tous les paiements.
     */
    @Query("""
        SELECT COUNT(sp) > 0 FROM ScheduledPayment sp
        WHERE sp.user.id = :userId
        AND sp.name = :name
        AND sp.amount = :amount
        AND sp.paymentMethod = :paymentMethod
        AND FUNCTION('DATE', sp.dueDate) = FUNCTION('DATE', :dueDate)
        AND sp.isPaid = false
        AND sp.isRecurring = true
        AND sp.deleted = false
        AND (:categoryId IS NULL AND sp.category IS NULL OR sp.category.id = :categoryId)
    """)
    boolean existsSimilarPayment(
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("amount") Double amount,
            @Param("paymentMethod") ma.siblhish.enums.PaymentMethod paymentMethod,
            @Param("dueDate") LocalDateTime dueDate,
            @Param("categoryId") Long categoryId);

}

