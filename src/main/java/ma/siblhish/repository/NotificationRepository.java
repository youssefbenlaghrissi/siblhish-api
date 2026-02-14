package ma.siblhish.repository;

import ma.siblhish.entities.Notification;
import ma.siblhish.enums.TypeNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
           "AND n.deleted = false " +
           "AND (:isRead IS NULL OR n.isRead = :isRead) " +
           "AND (:type IS NULL OR n.type = :type) " +
           "ORDER BY n.id DESC")
    Page<Notification> findNotificationsWithFilters(
            @Param("userId") Long userId,
            @Param("isRead") Boolean isRead,
            @Param("type") TypeNotification type,
            Pageable pageable);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false AND n.deleted = false")
    Long countUnreadByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.deleted = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);
    
    /**
     * Vérifie si une notification récente existe pour un paiement planifié.
     * Optimisé : utilise COUNT() au lieu de charger toutes les notifications.
     */
    @Query("""
        SELECT COUNT(n) > 0 FROM Notification n
        WHERE n.user.id = :userId
        AND n.type = :type
        AND n.creationDate > :since
        AND n.description LIKE CONCAT('%ID: ', :paymentIdStr, '%')
    """)
    boolean hasRecentNotificationForPayment(
            @Param("userId") Long userId,
            @Param("type") TypeNotification type,
            @Param("since") java.time.LocalDateTime since,
            @Param("paymentIdStr") String paymentIdStr);
}

