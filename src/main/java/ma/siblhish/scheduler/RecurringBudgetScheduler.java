package ma.siblhish.scheduler;

import lombok.RequiredArgsConstructor;
import ma.siblhish.entities.Budget;
import ma.siblhish.entities.Category;
import ma.siblhish.enums.TypeNotification;
import ma.siblhish.repository.BudgetRepository;
import ma.siblhish.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduler pour créer automatiquement les budgets récurrents chaque mois.
 * 
 * Un budget est considéré comme récurrent si :
 * - startDate = 1er jour d'un mois
 * - endDate = dernier jour du même mois
 * 
 * Exécution : Le 1er de chaque mois à 00:01:00
 */
@Component
@RequiredArgsConstructor
public class RecurringBudgetScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(RecurringBudgetScheduler.class);
    
    private final BudgetRepository budgetRepository;
    private final NotificationService notificationService;
    
    /**
     * Créer les budgets récurrents pour le mois en cours.
     * Exécuté le 1er de chaque mois à 00:01:00
     */
    @Scheduled(cron = "0 0 3 * * ?") // Le 1er de chaque mois à 00:01:00 : 0 1 0 1 * ?
    @Transactional
    public void createRecurringBudgetsForCurrentMonth() {
        logger.info("🔄 Démarrage de la création automatique des budgets récurrents pour le mois en cours");
        
        try {
            YearMonth currentMonth = YearMonth.now();
            LocalDate firstDayOfMonth = currentMonth.atDay(1);
            LocalDate lastDayOfMonth = currentMonth.atEndOfMonth();

            List<Budget> recurringBudgets = budgetRepository.findByIsRecurringTrueOrderByIdDesc();

            List<Budget> budgetsToCreate = new ArrayList<>();
            List<NotificationRequest> notificationsToCreate = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Budget templateBudget : recurringBudgets) {
                Long userId = templateBudget.getUser().getId();
                Category category = templateBudget.getCategory();

                // Créer un nouveau budget pour ce mois
                Budget newBudget = new Budget();
                newBudget.setUser(templateBudget.getUser());
                newBudget.setAmount(templateBudget.getAmount());
                newBudget.setStartDate(firstDayOfMonth);
                newBudget.setEndDate(lastDayOfMonth);
                newBudget.setIsRecurring(true);
                newBudget.setCategory(category);
                newBudget.setCreationDate(now);

                budgetsToCreate.add(newBudget);
                
                notificationsToCreate.add(new NotificationRequest(
                    userId,
                    "Budget récurrent créé",
                    String.format("Un budget récurrent de %.2f MAD a été créé automatiquement pour le mois de %s.", 
                        templateBudget.getAmount(), currentMonth),
                    category.getName()
                ));
            }

            if (!budgetsToCreate.isEmpty()) {
                budgetRepository.saveAll(budgetsToCreate);
                logger.info("✅ {} budgets récurrents créés en batch", budgetsToCreate.size());
                
                for (NotificationRequest notificationRequest : notificationsToCreate) {
                    createRecurringBudgetNotification(
                        notificationRequest.userId(),
                        notificationRequest.title(),
                        notificationRequest.description(),
                        notificationRequest.categoryName()
                    );
                }
            } else {
                logger.info("⏭️ Aucun nouveau budget récurrent à créer");
            }
            logger.info("✅ Création automatique des budgets récurrents terminée avec succès");
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création automatique des budgets récurrents: {}", e.getMessage(), e);
        }
    }

    /**
     * Record pour stocker les informations de notification
     */
    private record NotificationRequest(
        Long userId,
        String title,
        String description,
        String categoryName
    ) {}

    /**
     * Crée une notification pour un budget récurrent créé automatiquement
     */
    private void createRecurringBudgetNotification(Long userId, String title, 
                                                   String description, String categoryName) {
        try {
            notificationService.createNotification(
                userId,
                title,
                description + " (" + categoryName + ")",
                TypeNotification.RECURRING_BUDGET,
                "BUDGET"
            );
            logger.debug("📬 Notification créée pour l'utilisateur {} - Budget récurrent ({})", userId, categoryName);
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création de la notification pour l'utilisateur {}: {}", 
                    userId, e.getMessage());
        }
    }

}

