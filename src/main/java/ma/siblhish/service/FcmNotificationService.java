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

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Service pour envoyer des notifications push via Firebase Cloud Messaging (FCM) API V1
 * Utilise Firebase Admin SDK (recommandé par Firebase)
 * 
 * Note: Ce service remplace FcmNotificationServiceV1
 */
@Slf4j
@Service
public class FcmNotificationService {

    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    @Value("${firebase.service-account-classpath:firebase/siblhish-app-firebase-adminsdk-fbsvc-05ce4c5f95.json}")
    private String serviceAccountClasspath;

    private FirebaseMessaging firebaseMessaging;

    // Constructeur pour vérifier que le bean est créé
    public FcmNotificationService() {
        log.info("🏗️ FcmNotificationService - Constructeur appelé");
    }

    @PostConstruct
    public void initialize() {
        log.info("🔧 Début de l'initialisation de Firebase...");
        log.info("🔧 Chemin du service account (classpath): {}", serviceAccountClasspath);
        log.info("🔧 Chemin du service account (fichier): {}", serviceAccountPath);
        
        try {
            // Essayer d'initialiser Firebase si ce n'est pas déjà fait
            if (FirebaseApp.getApps().isEmpty()) {
                log.info("🔧 Aucune instance Firebase trouvée, création d'une nouvelle instance...");
                FirebaseOptions options;

                // Option 1 : Fichier depuis le système de fichiers (pour production)
                if (serviceAccountPath != null && !serviceAccountPath.isEmpty()) {
                    log.info("🔧 Tentative d'initialisation depuis le fichier: {}", serviceAccountPath);
                    FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);
                    GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
                    options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .build();
                    log.info("✅ Firebase initialisé depuis le fichier: {}", serviceAccountPath);
                }
                // Option 2 : Fichier depuis classpath (pour développement)
                else {
                    log.info("🔧 Tentative d'initialisation depuis classpath: {}", serviceAccountClasspath);
                    ClassPathResource resource = new ClassPathResource(serviceAccountClasspath);
                    if (resource.exists()) {
                        log.info("🔧 Fichier trouvé, lecture des credentials...");
                        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());
                        log.info("🔧 Credentials lus, création des options Firebase...");
                        options = FirebaseOptions.builder()
                                .setCredentials(credentials)
                                .build();
                        log.info("✅ Firebase initialisé depuis classpath: {}", serviceAccountClasspath);
                    } else {
                        log.error("❌ Fichier Firebase service account non trouvé: {}. Les notifications push ne fonctionneront pas.", serviceAccountClasspath);
                        log.error("❌ Vérifiez que le fichier existe dans: src/main/resources/{}", serviceAccountClasspath);
                        return;
                    }
                }

                log.info("🔧 Initialisation de FirebaseApp...");
                FirebaseApp.initializeApp(options);
                log.info("✅ FirebaseApp initialisé avec succès");
            } else {
                log.info("🔧 Instance Firebase déjà existante, réutilisation...");
            }

            log.info("🔧 Récupération de l'instance FirebaseMessaging...");
            firebaseMessaging = FirebaseMessaging.getInstance();
            log.info("✅ Firebase Messaging initialisé avec succès");

        } catch (IOException e) {
            log.error("❌ Erreur IO lors de l'initialisation de Firebase: {}", e.getMessage(), e);
            log.error("❌ Stack trace:", e);
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de l'initialisation de Firebase: {}", e.getMessage(), e);
            log.error("❌ Type d'erreur: {}", e.getClass().getName());
            log.error("❌ Stack trace:", e);
        }
        
        if (firebaseMessaging == null) {
            log.error("❌ CRITIQUE: FirebaseMessaging est null après l'initialisation. Les notifications push ne fonctionneront pas.");
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
            log.warn("⚠️ Firebase Messaging non initialisé. Impossible d'envoyer la notification.");
            return false;
        }

        // Vérifier que l'utilisateur a un token FCM et que les notifications sont activées
        if (user.getFcmToken() == null || user.getFcmToken().trim().isEmpty()) {
            log.warn("⚠️ Utilisateur {} n'a pas de token FCM, notification ignorée. Le token FCM doit être enregistré via POST /users/{userId}/fcm-token", user.getId());
            return false;
        }

        if (Boolean.FALSE.equals(user.getNotificationsEnabled())) {
            log.info("⚠️ Utilisateur {} a désactivé les notifications, notification ignorée", user.getId());
            return false;
        }
        
        log.info("📤 Tentative d'envoi de notification push à l'utilisateur {} avec le token: {}...", 
            user.getId(), user.getFcmToken().substring(0, Math.min(20, user.getFcmToken().length())));

        try {
            // Construire le message avec notification payload (RECOMMANDÉ)
            // FCM affiche automatiquement la notification même en arrière-plan
            Message.Builder messageBuilder = Message.builder()
                    .setToken(user.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            // Ajouter les données personnalisées si présentes
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            // Envoyer le message
            String response = firebaseMessaging.send(messageBuilder.build());
            log.info("✅ Notification envoyée avec succès à l'utilisateur {}: {} (messageId: {})", 
                user.getId(), title, response);
            return true;

        } catch (FirebaseMessagingException e) {
            log.error("❌ Erreur FCM lors de l'envoi à l'utilisateur {}: {}", user.getId(), e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de l'envoi à l'utilisateur {}: {}", user.getId(), e.getMessage(), e);
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

