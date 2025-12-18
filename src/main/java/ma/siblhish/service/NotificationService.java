package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Notification;
import ma.siblhish.entities.User;
import ma.siblhish.enums.TypeNotification;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.NotificationRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EntityMapper mapper;

    public PageResponseDto<NotificationDto> getNotifications(Long userId, Boolean isRead, 
                                                             TypeNotification type, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findNotificationsWithFilters(
                userId, isRead, type, pageable);
        
        PageResponseDto<NotificationDto> response = mapper.toPageResponseDto(
                notifications.map(mapper::toNotificationDto));
        return response;
    }

    @Transactional
    public NotificationDto markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
        
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
        notificationRepository.delete(notification);
    }

    public UnreadCountDto getUnreadCount(Long userId) {
        Long count = notificationRepository.countUnreadByUserId(userId);
        return new UnreadCountDto(count != null ? count.intValue() : 0);
    }

    @Transactional
    public void createNotification(Long userId, String title, String description, TypeNotification type) {
        createNotification(userId, title, description, type, null);
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
        LocalDateTime now = java.time.LocalDateTime.now();
        notification.setCreationDate(now);
        notification.setUpdateDate(now);
        
        notificationRepository.save(notification);
    }
}

