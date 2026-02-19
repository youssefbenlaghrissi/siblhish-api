package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Notification;
import ma.siblhish.entities.User;
import ma.siblhish.enums.TypeNotification;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.NotificationRepository;
import ma.siblhish.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EntityMapper mapper;
    private final FcmNotificationService fcmNotificationService;

    public List<NotificationDto> getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findAllByUserIdAndNotDeleted(userId);
        return notifications.stream()
                .map(mapper::toNotificationDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationDto markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
        
        Long userId = notification.getUser().getId();
        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapper.toNotificationDto(saved);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
        notification.setDeleted(true);
        notificationRepository.save(notification);
    }

    public UnreadCountDto getUnreadCount(Long userId) {
        Long count = notificationRepository.countUnreadByUserId(userId);
        return new UnreadCountDto(count != null ? count.intValue() : 0);
    }

    @Transactional
    public void createNotification(Long userId, String title, String description, TypeNotification type, String transactionType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setType(type);
        notification.setTransactionType(transactionType);
        notification.setIsRead(false);
        notification.setUser(user);
        LocalDateTime now = LocalDateTime.now();
        notification.setCreationDate(now);
        
        Notification savedNotification = notificationRepository.save(notification);
        // Envoyer une notification push à l'utilisateur de manière asynchrone
        sendNotificationAsync(user, title, description, type, transactionType, savedNotification.getId());
    }

    /**
     * Envoie une notification push de manière asynchrone.
     * Ne bloque pas le thread principal.
     */
    @Async
    public void sendNotificationAsync(User user, String title, String description, TypeNotification type, 
                                     String transactionType, Long notificationId) {
        try {
            log.info("📬 Création de notification pour l'utilisateur {} - Titre: {}, Description: {}", 
                user.getId(), title, description);
            log.info("📬 Token FCM de l'utilisateur: {}", 
                user.getFcmToken() != null && !user.getFcmToken().trim().isEmpty() 
                    ? user.getFcmToken().substring(0, Math.min(20, user.getFcmToken().length())) + "..." 
                    : "NULL ou VIDE");
            
            // Préparer les données supplémentaires pour la notification push
            Map<String, String> data = new HashMap<>();
            data.put("type", "NOTIFICATION");
            data.put("notificationId", notificationId.toString());
            data.put("notificationType", type.toString());
            if (transactionType != null) {
                data.put("transactionType", transactionType);
            }
            
            // Envoyer la notification push
            boolean sent = fcmNotificationService.sendNotification(user, title, description != null ? description : title, data);
            if (sent) {
                log.info("✅ Notification push envoyée avec succès pour l'utilisateur {}", user.getId());
            } else {
                log.warn("⚠️ Échec de l'envoi de la notification push pour l'utilisateur {}", user.getId());
            }
        } catch (Exception e) {
            // Ne pas faire échouer la création de la notification si l'envoi push échoue
            // On log juste l'erreur
            log.error("❌ Erreur lors de l'envoi de la notification push pour l'utilisateur {}: {}", 
                user.getId(), e.getMessage(), e);
        }
    }
}

