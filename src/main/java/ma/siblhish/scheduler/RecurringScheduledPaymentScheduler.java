package ma.siblhish.scheduler;

import lombok.RequiredArgsConstructor;
import ma.siblhish.entities.ScheduledPayment;
import ma.siblhish.enums.RecurrenceFrequency;
import ma.siblhish.enums.TypeNotification;
import ma.siblhish.repository.ScheduledPaymentRepository;
import ma.siblhish.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scheduler pour créer automatiquement les prochains paiements planifiés récurrents.
 * 
 * Logique :
 * - Récupère tous les paiements planifiés récurrents qui sont payés
 * - Pour chaque paiement payé, crée le prochain paiement selon la fréquence
 * - Vérifie la date limite (recurrenceEndDate)
 * - Vérifie si le prochain paiement existe déjà pour éviter les doublons
 * 
 * Exécution : Tous les jours à 04:00
 */
@Component
@RequiredArgsConstructor
public class RecurringScheduledPaymentScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(RecurringScheduledPaymentScheduler.class);
    
    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final NotificationService notificationService;
    
    /**
     * Créer les prochains paiements planifiés récurrents.
     * Exécuté tous les jours à 04:00
     */
    @Scheduled(cron = "0 0 1 * * ?") // Tous les jours à 04:00
    @Transactional
    public void createNextRecurringPayments() {
        createNextRecurringPaymentsInternal();
    }
    
    /**
     * Méthode publique pour déclencher manuellement la création des prochains paiements récurrents
     * Utile pour les tests ou déclenchement manuel via API
     */
    @Transactional
    public void createNextRecurringPaymentsInternal() {
        logger.info("🔄 Démarrage de la création automatique des prochains paiements planifiés récurrents");
        
        try {
            LocalDateTime now = LocalDateTime.now();

            List<ScheduledPayment> recurringPayments = scheduledPaymentRepository.findRecurringPaymentsToProcess(now);
            
            int paymentsCreated = 0;
            
            for (ScheduledPayment payment : recurringPayments) {
                try {
                    // Calculer la date du prochain paiement (fréquence + dayOfMonth, dayOfYear, daysOfWeek)
                    LocalDateTime nextDueDate = calculateNextDueDate(payment);
                    
                    // Vérifier la date limite
                    if (payment.getRecurrenceEndDate() != null 
                            && nextDueDate.isAfter(payment.getRecurrenceEndDate())) {
                        logger.debug("⏭️  Paiement récurrent {} a atteint sa date limite", payment.getId());
                        continue;
                    }
                    

                    // Créer le prochain paiement
                    ScheduledPayment nextPayment = createNextPayment(payment, nextDueDate);
                    scheduledPaymentRepository.save(nextPayment);
                    paymentsCreated++;
                    
                    // Créer une notification pour l'utilisateur (même format de description que ScheduledPaymentReminderScheduler)
                    String description = buildRecurringCreatedDescription(payment, nextDueDate);
                    createRecurringScheduledPaymentNotification(
                        payment.getUser().getId(),
                        "📅 Paiement planifié récurrent créé",
                        description,
                        null
                    );
                    
                    logger.debug("✅ Prochain paiement créé : {} - Due: {} (Paiement précédent: {} - Payé: {})", 
                            nextPayment.getName(), nextDueDate, 
                            payment.getDueDate(), Boolean.TRUE.equals(payment.getIsPaid()));
                    
                } catch (Exception e) {
                    logger.error("❌ Erreur lors de la création du prochain paiement pour le paiement ID: {}", 
                            payment.getId(), e);
                }
            }
            
            logger.info("✅ Création automatique terminée: {} prochains paiements créés", paymentsCreated);
            
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création automatique des paiements planifiés récurrents: {}", 
                    e.getMessage(), e);
        }
    }
    
    /**
     * Calcule la date d'échéance du prochain paiement en tenant compte de la fréquence,
     * recurrenceDayOfMonth, recurrenceDayOfYear et recurrenceDaysOfWeek (comme dépense/revenu).
     */
    private LocalDateTime calculateNextDueDate(ScheduledPayment payment) {
        LocalDateTime dueDate = payment.getDueDate();
        RecurrenceFrequency frequency = payment.getRecurrenceFrequency();

        return switch (frequency) {
            case DAILY -> dueDate.plusDays(1);

            case WEEKLY -> {
                List<Integer> daysOfWeek = payment.getRecurrenceDaysOfWeek();
                int currentDayOfWeek = dueDate.getDayOfWeek().getValue();
                List<Integer> sorted = new ArrayList<>(daysOfWeek);
                Collections.sort(sorted);
                Integer nextDay = sorted.stream().filter(d -> d > currentDayOfWeek).findFirst().orElse(null);
                int daysToAdd = nextDay != null
                        ? nextDay - currentDayOfWeek
                        : (7 - currentDayOfWeek) + sorted.getFirst();
                yield dueDate.plusDays(daysToAdd);
            }

            case MONTHLY -> {
                Integer dayOfMonth = payment.getRecurrenceDayOfMonth();
                LocalDate nextMonth = dueDate.toLocalDate().plusMonths(1);
                if (dayOfMonth != null) {
                    int day = Math.min(dayOfMonth, nextMonth.lengthOfMonth());
                    yield nextMonth.withDayOfMonth(day).atTime(dueDate.toLocalTime());
                }
                yield dueDate.plusMonths(1);
            }

            case YEARLY -> {
                Integer dayOfYear = payment.getRecurrenceDayOfYear();
                int nextYear = dueDate.getYear() + 1;
                if (dayOfYear != null) {
                    int yearLength = LocalDate.of(nextYear, 12, 31).lengthOfYear();
                    int d = Math.min(dayOfYear, yearLength);
                    LocalDate target = LocalDate.of(nextYear, 1, 1).plusDays(d - 1);
                    yield target.atTime(dueDate.toLocalTime());
                }
                yield dueDate.plusYears(1);
            }
        };
    }

    /**
     * Crée le prochain paiement planifié basé sur le paiement payé
     */
    private ScheduledPayment createNextPayment(ScheduledPayment template, LocalDateTime nextDueDate) {
        ScheduledPayment nextPayment = new ScheduledPayment();
        nextPayment.setName(template.getName());
        nextPayment.setAmount(template.getAmount());
        nextPayment.setPaymentMethod(template.getPaymentMethod());
        nextPayment.setBeneficiary(template.getBeneficiary());
        nextPayment.setIsRecurring(true);
        nextPayment.setRecurrenceFrequency(template.getRecurrenceFrequency());
        nextPayment.setRecurrenceEndDate(template.getRecurrenceEndDate());
        
        // Créer une nouvelle liste pour éviter le partage de référence (erreur Hibernate)
        if (template.getRecurrenceDaysOfWeek() != null) {
            nextPayment.setRecurrenceDaysOfWeek(new ArrayList<>(template.getRecurrenceDaysOfWeek()));
        }
        nextPayment.setRecurrenceDayOfMonth(template.getRecurrenceDayOfMonth());
        nextPayment.setRecurrenceDayOfYear(template.getRecurrenceDayOfYear());
        nextPayment.setNotificationOption(template.getNotificationOption());
        nextPayment.setIsPaid(false);
        nextPayment.setUser(template.getUser());
        nextPayment.setCategory(template.getCategory());
        nextPayment.setDueDate(nextDueDate);
        nextPayment.setCreationDate(LocalDateTime.now());
        
        return nextPayment;
    }

    /**
     * Construit la description pour "paiement récurrent créé" (même structure que ScheduledPaymentReminderScheduler).
     */
    private String buildRecurringCreatedDescription(ScheduledPayment payment, LocalDateTime nextDueDate) {
        StringBuilder desc = new StringBuilder("📅 ");
        desc.append("Votre paiement planifié \"").append(payment.getName()).append("\"");
        if (payment.getCategory() != null) {
            String catName = payment.getCategory().getName();
            String catIcon = payment.getCategory().getIcon();
            desc.append(" catégorie ");
            desc.append(catName);
            if (catIcon != null && !catIcon.isBlank()) {
                desc.append(" ").append(catIcon).append(" ");
            }
        }
        desc.append("d'un montant de ");
        desc.append(String.format("%.2f", payment.getAmount()));
        desc.append(" MAD, ");
        desc.append("a été créé automatiquement avec une date d'échéance le ");
        desc.append(nextDueDate.toLocalDate().toString());
        if (payment.getBeneficiary() != null && !payment.getBeneficiary().trim().isEmpty()) {
            desc.append(" pour le bénéficiaire ").append(payment.getBeneficiary());
        }
        return desc.toString();
    }

    /**
     * Crée une notification pour un paiement planifié récurrent créé automatiquement
     */
    private void createRecurringScheduledPaymentNotification(Long userId, String title, 
                                                             String description, String categoryName) {
        try {
            notificationService.createNotification(
                userId,
                title,
                description != null ? description : "",
                TypeNotification.RECURRING_SCHEDULED_PAYMENT,
                "SCHEDULED_PAYMENT"
            );
            logger.debug("📬 Notification créée pour l'utilisateur {} - Paiement planifié récurrent", userId);
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création de la notification pour l'utilisateur {}: {}", 
                    userId, e.getMessage());
            // Ne pas bloquer la création du paiement si la notification échoue
        }
    }
}

