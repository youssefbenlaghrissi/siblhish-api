package ma.siblhish.controller;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.enums.TypeNotification;
import ma.siblhish.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller pour la gestion des notifications
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Liste des notifications
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<PageResponseDto<NotificationDto>>> getNotifications(
            @PathVariable Long userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) TypeNotification type,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        PageResponseDto<NotificationDto> notifications = notificationService.getNotifications(
                userId, isRead, type, page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Marquer une notification comme lue
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(@PathVariable Long notificationId) {
        NotificationDto notification = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    /**
     * Marquer toutes les notifications comme lues
     */
    @PatchMapping("/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }

    /**
     * Supprimer une notification
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtenir le nombre de notifications non lues
     */
    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountDto>> getUnreadCount(@PathVariable Long userId) {
        UnreadCountDto count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}

