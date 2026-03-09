package ma.siblhish.repository;

import ma.siblhish.entities.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
     * Trouve uniquement les templates récurrents (parent_scheduled_payment_id IS NULL).
     * Les occurrences créées par le batch ont parent_scheduled_payment_id renseigné donc ne sont pas retournées.
     */
    @Query("""
        SELECT DISTINCT sp FROM ScheduledPayment sp
        LEFT JOIN FETCH sp.category
        LEFT JOIN FETCH sp.user
        LEFT JOIN FETCH sp.recurrenceDaysOfWeek
        WHERE sp.isRecurring = true
        AND sp.recurrenceFrequency IS NOT NULL
        AND sp.parentScheduledPaymentId IS NULL
        AND sp.deleted = false
        AND sp.dueDate IS NOT NULL
    """)
    List<ScheduledPayment> findRecurringTemplates();

    /**
     * Max(due_date) par parent_scheduled_payment_id (une requête pour éviter N+1).
     * Résultat : [parentScheduledPaymentId, maxDueDate]
     */
    @Query("""
        SELECT sp.parentScheduledPaymentId, MAX(sp.dueDate)
        FROM ScheduledPayment sp
        WHERE sp.parentScheduledPaymentId IS NOT NULL
        AND sp.deleted = false
        GROUP BY sp.parentScheduledPaymentId
    """)
    List<Object[]> findMaxDueDateByParentId();

    /**
     * Vérifie si un paiement existe déjà pour ce template à cette date (template lui-même ou occurrence).
     * @deprecated Préférer {@link #findTemplateIdsWithPaymentOnDate(LocalDate)} pour éviter N requêtes dans le batch.
     */
    @Deprecated
    @Query("""
        SELECT COUNT(sp) > 0 FROM ScheduledPayment sp
        WHERE (sp.id = :templateId OR sp.parentScheduledPaymentId = :templateId)
        AND FUNCTION('DATE', sp.dueDate) = FUNCTION('DATE', :dueDate)
        AND sp.deleted = false
    """)
    boolean existsByParentAndDueDate(
            @Param("templateId") Long templateId,
            @Param("dueDate") LocalDateTime dueDate);

    /**
     * Une seule requête : tous les template IDs qui ont déjà un paiement (template ou occurrence) à la date donnée.
     * Évite N appels à existsByParentAndDueDate dans le batch.
     */
    @Query("""
        SELECT sp.id FROM ScheduledPayment sp
        WHERE sp.parentScheduledPaymentId IS NULL
        AND FUNCTION('DATE', sp.dueDate) = :date
        AND sp.deleted = false
        UNION
        SELECT sp.parentScheduledPaymentId FROM ScheduledPayment sp
        WHERE sp.parentScheduledPaymentId IS NOT NULL
        AND FUNCTION('DATE', sp.dueDate) = :date
        AND sp.deleted = false
    """)
    List<Long> findTemplateIdsWithPaymentOnDate(@Param("date") LocalDate date);

}

