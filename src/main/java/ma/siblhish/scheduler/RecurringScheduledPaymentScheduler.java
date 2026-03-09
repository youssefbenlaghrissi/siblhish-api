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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scheduler pour créer automatiquement les prochains paiements planifiés récurrents.
 *
 * Approche template_id (parent_scheduled_payment_id) :
 * - On ne traite que les modèles (parent_scheduled_payment_id IS NULL).
 * - Les occurrences créées ont parent_scheduled_payment_id = id du template et isRecurring = false.
 * - Prochaine date = calculateNextDueDate(template, lastDue) avec lastDue = max(due_date) de la série.
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
    @Scheduled(cron = "0 18 2 * * ?")
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
            LocalDate today = LocalDate.now();
            List<ScheduledPayment> templates = scheduledPaymentRepository.findRecurringTemplates();
            Map<Long, LocalDateTime> maxDueByParentId = buildMaxDueByParentIdMap();
            Set<Long> templateIdsWithPaymentToday = new HashSet<>(
                    scheduledPaymentRepository.findTemplateIdsWithPaymentOnDate(today));
            List<ScheduledPayment> toCreate = new ArrayList<>();

            for (ScheduledPayment template : templates) {
                try {
                    LocalDateTime lastDue = template.getDueDate();
                    LocalDateTime maxFromOccurrences = maxDueByParentId.get(template.getId());
                    if (maxFromOccurrences != null && (lastDue == null || maxFromOccurrences.isAfter(lastDue))) {
                        lastDue = maxFromOccurrences;
                    }
                    if (lastDue == null) {
                        continue;
                    }

                    LocalDateTime nextDueDate = calculateNextDueDate(template, lastDue);

                    if (template.getRecurrenceEndDate() != null
                            && nextDueDate.toLocalDate().isAfter(template.getRecurrenceEndDate().toLocalDate())) {
                        logger.debug("⏭️ Paiement récurrent {} a atteint sa date limite", template.getId());
                        continue;
                    }

                    if (!today.equals(nextDueDate.toLocalDate())) {
                        continue;
                    }

                    if (templateIdsWithPaymentToday.contains(template.getId())) {
                        logger.debug("⏭️ Paiement déjà présent pour template {} - échéance {}", template.getId(), nextDueDate.toLocalDate());
                        continue;
                    }

                    ScheduledPayment nextPayment = createOccurrenceFromTemplate(template, nextDueDate);
                    toCreate.add(nextPayment);
                    templateIdsWithPaymentToday.add(template.getId());
                } catch (Exception e) {
                    logger.error("❌ Erreur lors de la création pour le template ID: {}", template.getId(), e);
                }
            }

            if (!toCreate.isEmpty()) {
                scheduledPaymentRepository.saveAll(toCreate);
                for (ScheduledPayment payment : toCreate) {
                    String description = buildRecurringCreatedDescription(payment, payment.getDueDate());
                    createRecurringScheduledPaymentNotification(
                            payment.getUser().getId(),
                            "📅 Paiement planifié récurrent créé",
                            description,
                            null
                    );
                    logger.debug("✅ Occurrence créée : {} - Due: {} (parent: {})", payment.getName(), payment.getDueDate(), payment.getParentScheduledPaymentId());
                }
            }

            logger.info("✅ Création automatique terminée: {} prochains paiements créés", toCreate.size());

        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création automatique des paiements planifiés récurrents: {}",
                    e.getMessage(), e);
        }
    }

    private Map<Long, LocalDateTime> buildMaxDueByParentIdMap() {
        List<Object[]> rows = scheduledPaymentRepository.findMaxDueDateByParentId();
        Map<Long, LocalDateTime> map = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            Long parentId = (Long) row[0];
            LocalDateTime maxDue = (LocalDateTime) row[1];
            if (parentId != null && maxDue != null) {
                map.merge(parentId, maxDue, (a, b) -> a.isAfter(b) ? a : b);
            }
        }
        return map;
    }

    /**
     * Calcule la prochaine date d'échéance à partir de la date de référence (dernière dans la série).
     */
    private LocalDateTime calculateNextDueDate(ScheduledPayment template, LocalDateTime fromDate) {
        RecurrenceFrequency frequency = template.getRecurrenceFrequency();
        if (frequency == null) {
            return fromDate.plusDays(1);
        }
        return switch (frequency) {
            case DAILY -> fromDate.plusDays(1);

            case WEEKLY -> {
                List<Integer> daysOfWeek = template.getRecurrenceDaysOfWeek();
                if (daysOfWeek == null || daysOfWeek.isEmpty()) {
                    yield fromDate.plusDays(7);
                }
                int currentDayOfWeek = fromDate.getDayOfWeek().getValue();
                List<Integer> sorted = new ArrayList<>(daysOfWeek);
                Collections.sort(sorted);
                Integer nextDay = sorted.stream().filter(d -> d > currentDayOfWeek).findFirst().orElse(null);
                int daysToAdd = nextDay != null
                        ? nextDay - currentDayOfWeek
                        : (7 - currentDayOfWeek) + sorted.getFirst();
                yield fromDate.plusDays(daysToAdd);
            }

            case MONTHLY -> {
                Integer dayOfMonth = template.getRecurrenceDayOfMonth();
                LocalDate nextMonth = fromDate.toLocalDate().plusMonths(1);
                if (dayOfMonth != null) {
                    int day = Math.min(dayOfMonth, nextMonth.lengthOfMonth());
                    yield nextMonth.withDayOfMonth(day).atTime(fromDate.toLocalTime());
                }
                yield fromDate.plusMonths(1);
            }

            case YEARLY -> {
                Integer dayOfYear = template.getRecurrenceDayOfYear();
                int nextYear = fromDate.getYear() + 1;
                if (dayOfYear != null) {
                    int yearLength = LocalDate.of(nextYear, 12, 31).lengthOfYear();
                    int d = Math.min(dayOfYear, yearLength);
                    LocalDate target = LocalDate.of(nextYear, 1, 1).plusDays(d - 1);
                    yield target.atTime(fromDate.toLocalTime());
                }
                yield fromDate.plusYears(1);
            }
        };
    }

    /**
     * Crée une occurrence avec parent_scheduled_payment_id = template.getId() et isRecurring = false.
     */
    private ScheduledPayment createOccurrenceFromTemplate(ScheduledPayment template, LocalDateTime nextDueDate) {
        ScheduledPayment nextPayment = new ScheduledPayment();
        nextPayment.setName(template.getName());
        nextPayment.setAmount(template.getAmount());
        nextPayment.setPaymentMethod(template.getPaymentMethod());
        nextPayment.setBeneficiary(template.getBeneficiary());
        nextPayment.setIsRecurring(false);
        nextPayment.setRecurrenceFrequency(null);
        nextPayment.setRecurrenceEndDate(null);
        nextPayment.setRecurrenceDaysOfWeek(null);
        nextPayment.setRecurrenceDayOfMonth(null);
        nextPayment.setRecurrenceDayOfYear(null);
        nextPayment.setParentScheduledPaymentId(template.getId());
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
        StringBuilder desc = new StringBuilder();
        String paymentName = payment.getName() != null ? payment.getName() : "Paiement planifié";
        desc.append("Votre paiement planifié \"").append(paymentName).append("\"");
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

