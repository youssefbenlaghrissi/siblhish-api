package ma.siblhish.scheduler;

import lombok.RequiredArgsConstructor;
import ma.siblhish.entities.ScheduledPayment;
import ma.siblhish.enums.RecurrenceFrequency;
import ma.siblhish.repository.ScheduledPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    
    /**
     * Créer les prochains paiements planifiés récurrents.
     * Exécuté tous les jours à 04:00
     */
    @Scheduled(cron = "0 0 4 * * ?") // Tous les jours à 04:00
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
            
            // Récupérer tous les paiements planifiés récurrents qui nécessitent la création du prochain paiement
            // Conditions :
            // 1. isRecurring = true
            // 2. recurrenceFrequency défini
            // 3. Soit le paiement est payé, soit la date d'échéance est passée (pour continuer le cycle même si non payé)
            List<ScheduledPayment> recurringPayments = scheduledPaymentRepository.findAll().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsRecurring()))
                    .filter(p -> p.getRecurrenceFrequency() != null)
                    .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                    .filter(p -> {
                        // Inclure si :
                        // - Le paiement est payé, OU
                        // - La date d'échéance est passée (même si non payé, pour continuer le cycle)
                        return Boolean.TRUE.equals(p.getIsPaid()) 
                            || (p.getDueDate() != null && p.getDueDate().isBefore(now));
                    })
                    .toList();
            
            int paymentsCreated = 0;
            
            for (ScheduledPayment payment : recurringPayments) {
                try {
                    // Calculer la date du prochain paiement
                    LocalDateTime nextDueDate = calculateNextDueDate(
                            payment.getDueDate(),
                            payment.getRecurrenceFrequency()
                    );
                    
                    // Vérifier la date limite
                    if (payment.getRecurrenceEndDate() != null 
                            && nextDueDate.isAfter(payment.getRecurrenceEndDate())) {
                        logger.debug("⏭️  Paiement récurrent {} a atteint sa date limite", payment.getId());
                        continue;
                    }
                    
                    // Vérifier si le prochain paiement existe déjà
                    if (nextPaymentExists(payment, nextDueDate)) {
                        logger.debug("⏭️  Prochain paiement existe déjà pour le paiement récurrent {}", payment.getId());
                        continue;
                    }
                    
                    // Créer le prochain paiement
                    ScheduledPayment nextPayment = createNextPayment(payment, nextDueDate);
                    scheduledPaymentRepository.save(nextPayment);
                    paymentsCreated++;
                    
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
     * Calcule la date du prochain paiement selon la fréquence
     */
    private LocalDateTime calculateNextDueDate(LocalDateTime currentDueDate, RecurrenceFrequency frequency) {
        return switch (frequency) {
            case DAILY -> currentDueDate.plusDays(1);
            case WEEKLY -> currentDueDate.plusWeeks(1);
            case MONTHLY -> currentDueDate.plusMonths(1);
            case YEARLY -> currentDueDate.plusYears(1);
        };
    }
    
    /**
     * Vérifie si le prochain paiement existe déjà
     */
    private boolean nextPaymentExists(ScheduledPayment template, LocalDateTime nextDueDate) {
        // Vérifier s'il existe un paiement non payé avec les mêmes caractéristiques
        List<ScheduledPayment> existing = scheduledPaymentRepository.findAll().stream()
                .filter(p -> p.getUser().getId().equals(template.getUser().getId()))
                .filter(p -> p.getName().equals(template.getName()))
                .filter(p -> p.getAmount().equals(template.getAmount()))
                .filter(p -> p.getPaymentMethod().equals(template.getPaymentMethod()))
                .filter(p -> p.getDueDate().toLocalDate().equals(nextDueDate.toLocalDate()))
                .filter(p -> Boolean.FALSE.equals(p.getIsPaid()))
                .filter(p -> Boolean.TRUE.equals(p.getIsRecurring()))
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .toList();
        
        return !existing.isEmpty();
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
}

