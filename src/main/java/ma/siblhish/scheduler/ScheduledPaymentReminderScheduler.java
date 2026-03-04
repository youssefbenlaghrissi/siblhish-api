package ma.siblhish.scheduler;

import lombok.RequiredArgsConstructor;
import ma.siblhish.entities.ScheduledPayment;
import ma.siblhish.enums.NotificationOption;
import ma.siblhish.enums.TypeNotification;
import ma.siblhish.repository.NotificationRepository;
import ma.siblhish.repository.ScheduledPaymentRepository;
import ma.siblhish.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler pour envoyer des notifications de rappel pour les paiements planifiés.
 * 
 * Fréquence d'exécution recommandée :
 * - Option 1 : Quotidien à 08:00 (recommandé) - Vérifie tous les paiements du jour et des jours suivants
 * - Option 2 : Deux fois par jour (08:00 et 20:00) - Pour plus de réactivité
 * - Option 3 : Toutes les 6 heures - Pour une réactivité maximale
 * 
 * Logique :
 * - Vérifie les paiements non payés avec notificationOption != NONE
 * - Envoie des notifications selon l'option :
 *   - THREE_DAYS_BEFORE : 3 jours avant la date d'échéance
 *   - ONE_DAY_BEFORE : 1 jour avant la date d'échéance
 *   - ON_DUE_DATE : Le jour même de l'échéance
 * - Évite les doublons en vérifiant les notifications récentes (24h)
 * 
 * Exécution : Tous les jours à 08:00 (recommandé)
 */
@Component
@RequiredArgsConstructor
public class ScheduledPaymentReminderScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledPaymentReminderScheduler.class);
    
    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void sendPaymentReminders() {
        sendPaymentRemindersInternal();
    }
    
    /**
     * Méthode publique pour déclencher manuellement l'envoi des rappels
     * Utile pour les tests ou déclenchement manuel via API
     */
    @Transactional
    public void sendPaymentRemindersInternal() {
        logger.info("🔄 Démarrage de l'envoi des notifications de rappel pour les paiements planifiés");
        
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime now = LocalDateTime.now();
            
            // OPTIMISATION : Requête spécifique au lieu de findAll()
            List<ScheduledPayment> paymentsToNotify = scheduledPaymentRepository.findPaymentsToNotify(now);
            
            int notificationsSent = 0;
            
            for (ScheduledPayment payment : paymentsToNotify) {
                try {
                    LocalDate dueDate = payment.getDueDate().toLocalDate();
                    long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
                    
                    boolean shouldSendNotification = false;
                    String reminderType = "";
                    
                    // Vérifier d'abord si le paiement est en retard
                    if (daysUntilDue < 0) {
                        // Paiement en retard
                        shouldSendNotification = true;
                        reminderType = "en retard";
                    } else {
                        // Déterminer si une notification doit être envoyée selon l'option
                        switch (payment.getNotificationOption()) {
                            case THREE_DAYS_BEFORE:
                                if (daysUntilDue == 3) {
                                    shouldSendNotification = true;
                                    reminderType = "3 jours";
                                }
                                break;
                            case ONE_DAY_BEFORE:
                                if (daysUntilDue == 1) {
                                    shouldSendNotification = true;
                                    reminderType = "1 jour";
                                }
                                break;
                            case ON_DUE_DATE:
                                if (daysUntilDue == 0) {
                                    shouldSendNotification = true;
                                    reminderType = "aujourd'hui";
                                }
                                break;
                            case NONE:
                                // Ne devrait pas arriver ici grâce au filtre (sauf si en retard)
                                break;
                        }
                    }
                    
                    // Vérifier si une notification a déjà été envoyée récemment (dans les 24h) pour éviter les doublons
                    if (shouldSendNotification && !hasRecentNotification(payment, payment.getUser().getId(), reminderType, daysUntilDue)) {
                        sendReminderNotification(payment, reminderType, daysUntilDue);
                        notificationsSent++;
                    }
                    
                } catch (Exception e) {
                    logger.error("❌ Erreur lors du traitement du paiement planifié ID: {}", 
                            payment.getId(), e);
                }
            }
            
            logger.info("✅ Envoi des notifications terminé: {} notifications envoyées", notificationsSent);
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi des notifications de rappel: {}", 
                    e.getMessage(), e);
        }
    }
    
    /**
     * Vérifie si une notification a déjà été envoyée récemment pour ce paiement.
     *
     * On considère comme doublon une notification :
     * - du même utilisateur
     * - du même type fonctionnel (PAYMENT_OVERDUE, PAYMENT_DUE_TODAY, PAYMENT_REMINDER)
     * - avec exactement la même description
     * - créée dans les dernières 24h.
     *
     * Cela évite d'envoyer plusieurs fois d'affilée la même notif si le batch
     * est relancé manuellement dans la même journée.
     */
    private boolean hasRecentNotification(ScheduledPayment payment,
                                          Long userId,
                                          String reminderType,
                                          long daysUntilDue) {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        TypeNotification notificationType = determineNotificationType(reminderType);
        String description = buildReminderDescription(payment, daysUntilDue);

        return notificationRepository.hasRecentNotificationForPayment(
                userId,
                notificationType,
                yesterday,
                description
        );
    }
    
    /**
     * Détermine le type de notification selon le type de rappel
     */
    private TypeNotification determineNotificationType(String reminderType) {
        if ("en retard".equals(reminderType)) {
            return TypeNotification.PAYMENT_OVERDUE;
        } else if ("aujourd'hui".equals(reminderType)) {
            return TypeNotification.PAYMENT_DUE_TODAY;
        }
        return TypeNotification.PAYMENT_REMINDER;
    }
    
    /**
     * Envoie une notification de rappel pour un paiement planifié
     */
    private void sendReminderNotification(ScheduledPayment payment, String reminderType, long daysUntilDue) {
        try {
            TypeNotification notificationType;
            String title;
            
            // Déterminer le type de notification selon le contexte
            if (daysUntilDue < 0) {
                // Paiement en retard
                notificationType = TypeNotification.PAYMENT_OVERDUE;
                title = "⚠️ Paiement en retard";
            } else if (daysUntilDue == 0) {
                // Paiement dû aujourd'hui
                notificationType = TypeNotification.PAYMENT_DUE_TODAY;
                title = "📅 Paiement dû aujourd'hui";
            } else {
                // Rappel avant échéance (icône différente de « Paiement dû aujourd'hui »)
                notificationType = TypeNotification.PAYMENT_REMINDER;
                title = "🔔 Rappel de paiement planifié";
            }
            
            String description = buildReminderDescription(payment, daysUntilDue);
            
            notificationService.createNotification(
                payment.getUser().getId(),
                title,
                description,
                notificationType,
                "PAYMENT_REMINDER"
            );
            
            logger.debug("📬 Notification de rappel envoyée pour le paiement {} (ID: {}) - {} avant l'échéance", 
                    payment.getName(), payment.getId(), reminderType);
                    
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi de la notification pour le paiement ID: {}", 
                    payment.getId(), e);
            // Ne pas bloquer le processus si une notification échoue
        }
    }
    
    /**
     * Construit la description de la notification de rappel
     * Icône 📉 = paiement (sortie d'argent), cohérent avec les notifs dépense/revenu
     */
    private String buildReminderDescription(ScheduledPayment payment, long daysUntilDue) {
        StringBuilder desc = new StringBuilder();
        String paymentName = payment.getName() != null ? payment.getName() : "Paiement planifié";

        if (daysUntilDue < 0) {
            desc.append("Votre paiement planifié \"").append(paymentName).append("\"");
        } else if (daysUntilDue == 0) {
            desc.append("Votre paiement planifié \"").append(paymentName).append("\"");
        } else {
            desc.append("Rappel : Votre paiement planifié \"").append(paymentName).append("\"");
        }

        if (payment.getCategory() != null) {
            String catName = payment.getCategory().getName();
            String catIcon = payment.getCategory().getIcon();
            desc.append(" catégorie ");
            desc.append(catName);
            if (catIcon != null && !catIcon.isBlank()) {
                desc.append(" ").append(catIcon).append(" ");
            }
        }
        desc.append(", d'un montant de ");
        desc.append(String.format("%.2f", payment.getAmount()));
        desc.append(" MAD, ");
        
        if (daysUntilDue < 0) {
            long daysOverdue = Math.abs(daysUntilDue);
            desc.append("était dû il y a ").append(daysOverdue);
            if (daysOverdue == 1) {
                desc.append(" jour");
            } else {
                desc.append(" jours");
            }
        } else if (daysUntilDue == 0) {
            desc.append("est dû aujourd'hui");
        } else if (daysUntilDue == 1) {
            desc.append("est dû demain");
        } else {
            desc.append("est dû dans ").append(daysUntilDue).append(" jours");
        }
        
        desc.append(" (échéance : ").append(payment.getDueDate().toLocalDate().toString()).append(")");
        
        if (payment.getBeneficiary() != null && !payment.getBeneficiary().trim().isEmpty()) {
            desc.append(" pour le bénéficiaire ").append(payment.getBeneficiary());
        }

        return desc.toString();
    }
}

