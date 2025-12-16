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
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    
    Page<Notification> findByUserIdAndIsRead(Long userId, Boolean isRead, Pageable pageable);
    
    Page<Notification> findByUserIdAndType(Long userId, TypeNotification type, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId " +
           "AND (:isRead IS NULL OR n.isRead = :isRead) " +
           "AND (:type IS NULL OR n.type = :type)")
    Page<Notification> findNotificationsWithFilters(
            @Param("userId") Long userId,
            @Param("isRead") Boolean isRead,
            @Param("type") TypeNotification type,
            Pageable pageable);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") Long userId);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllAsReadByUserId(@Param("userId") Long userId);
}

