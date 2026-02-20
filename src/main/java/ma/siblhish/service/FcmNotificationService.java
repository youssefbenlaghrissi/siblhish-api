package ma.siblhish.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import ma.siblhish.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Service pour envoyer des notifications push via Firebase Cloud Messaging (FCM) API V1.
 * Utilise Firebase Admin SDK.
 *
 * Configuration : variable d'environnement FIREBASE_SERVICE_ACCOUNT_JSON
 * (contenu complet du fichier JSON du Service Account Firebase).
 * À définir en local et en production (ex. Railway).
 */
@Slf4j
@Service
public class FcmNotificationService {

    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson;

    private FirebaseMessaging firebaseMessaging;

    @PostConstruct
    public void initialize() {
        log.info("🔧 Initialisation Firebase (FIREBASE_SERVICE_ACCOUNT_JSON)...");
        boolean hasJson = serviceAccountJson != null && !serviceAccountJson.isBlank();
        log.info("🔧 Variable FIREBASE_SERVICE_ACCOUNT_JSON: {}", hasJson ? "définie" : "non définie");

        if (!hasJson) {
            log.warn("⚠️ FIREBASE_SERVICE_ACCOUNT_JSON non défini. Les notifications push seront désactivées.");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)));
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase initialisé");
            }
            firebaseMessaging = FirebaseMessaging.getInstance();
            log.info("✅ Firebase Messaging prêt");
        } catch (IOException e) {
            log.error("❌ Erreur initialisation Firebase: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Erreur inattendue Firebase: {}", e.getMessage(), e);
        }

        if (firebaseMessaging == null) {
            log.warn("⚠️ Firebase Messaging non initialisé. Définir FIREBASE_SERVICE_ACCOUNT_JSON pour activer les push.");
        }
    }

    /**
     * Envoie une notification push à un utilisateur.
     */
    public boolean sendNotification(User user, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.warn("⚠️ Firebase Messaging non initialisé. Impossible d'envoyer la notification.");
            return false;
        }
        if (user.getFcmToken() == null || user.getFcmToken().trim().isEmpty()) {
            log.warn("⚠️ Utilisateur {} sans token FCM, notification ignorée.", user.getId());
            return false;
        }
        if (Boolean.FALSE.equals(user.getNotificationsEnabled())) {
            log.debug("Utilisateur {} a désactivé les notifications.", user.getId());
            return false;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(user.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }
            String messageId = firebaseMessaging.send(messageBuilder.build());
            log.info("✅ Notification envoyée à l'utilisateur {}: {} (messageId: {})", user.getId(), title, messageId);
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("❌ Erreur FCM pour utilisateur {}: {}", user.getId(), e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ Erreur envoi notification pour utilisateur {}: {}", user.getId(), e.getMessage(), e);
            return false;
        }
    }

    public boolean sendNotification(User user, String title, String body) {
        return sendNotification(user, title, body, null);
    }
}
