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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service pour envoyer des notifications push via Firebase Cloud Messaging (FCM) API V1
 * Utilise Firebase Admin SDK (recommandé par Firebase)
 */
@Slf4j
@Service
public class FcmNotificationServiceV1 {

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    @Value("${firebase.service-account-classpath:firebase-service-account.json}")
    private String serviceAccountClasspath;

    private FirebaseMessaging firebaseMessaging;

    @PostConstruct
    public void initialize() {
        try {
            // Essayer d'initialiser Firebase si ce n'est pas déjà fait
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options;

                // Option 1 : Fichier depuis le système de fichiers (pour production)
                if (serviceAccountPath != null && !serviceAccountPath.isEmpty()) {
                    FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);
                    GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
                    options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .build();
                    log.info("Firebase initialized from file: {}", serviceAccountPath);
                }
                // Option 2 : Fichier depuis classpath (pour développement)
                else {
                    ClassPathResource resource = new ClassPathResource(serviceAccountClasspath);
                    if (resource.exists()) {
                        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());
                        options = FirebaseOptions.builder()
                                .setCredentials(credentials)
                                .build();
                        log.info("Firebase initialized from classpath: {}", serviceAccountClasspath);
                    } else {
                        log.warn("Firebase service account file not found. Push notifications will not work.");
                        return;
                    }
                }

                FirebaseApp.initializeApp(options);
            }

            firebaseMessaging = FirebaseMessaging.getInstance();
            log.info("Firebase Messaging initialized successfully");

        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage(), e);
        }
    }

    /**
     * Envoie une notification push à un utilisateur
     * 
     * @param user L'utilisateur destinataire (doit avoir un fcmToken)
     * @param title Titre de la notification
     * @param body Corps de la notification
     * @param data Données supplémentaires (optionnel)
     * @return true si la notification a été envoyée avec succès, false sinon
     */
    public boolean sendNotification(User user, String title, String body, Map<String, String> data) {
        // Vérifier que Firebase est initialisé
        if (firebaseMessaging == null) {
            log.warn("Firebase Messaging not initialized. Cannot send push notification.");
            return false;
        }

        // Vérifier que l'utilisateur a un token FCM et que les notifications sont activées
        if (user.getFcmToken() == null || user.getFcmToken().trim().isEmpty()) {
            log.debug("User {} has no FCM token, skipping push notification", user.getId());
            return false;
        }

        if (Boolean.FALSE.equals(user.getNotificationsEnabled())) {
            log.debug("User {} has notifications disabled, skipping push notification", user.getId());
            return false;
        }

        try {
            // Construire le message
            Message.Builder messageBuilder = Message.builder()
                    .setToken(user.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            // Ajouter les données personnalisées
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            // Envoyer le message
            String response = firebaseMessaging.send(messageBuilder.build());
            log.info("Push notification sent successfully to user {}: {} (messageId: {})", user.getId(), title, response);
            return true;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send push notification to user {}: {}", user.getId(), e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Error sending push notification to user {}: {}", user.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envoie une notification push simple (sans données supplémentaires)
     */
    public boolean sendNotification(User user, String title, String body) {
        return sendNotification(user, title, body, null);
    }
}

