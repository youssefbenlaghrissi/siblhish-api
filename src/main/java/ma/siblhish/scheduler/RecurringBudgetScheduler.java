package ma.siblhish.scheduler;

import lombok.RequiredArgsConstructor;
import ma.siblhish.entities.Budget;
import ma.siblhish.entities.Category;
import ma.siblhish.enums.TypeNotification;
import ma.siblhish.repository.BudgetRepository;
import ma.siblhish.service.BudgetService;
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
import java.util.Set;
import java.util.stream.Collectors;

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
    @Scheduled(cron = "0 1 0 1 * ?") // Le 1er de chaque mois à 00:01:00
    @Transactional
    public void createRecurringBudgetsForCurrentMonth() {
        logger.info("🔄 Démarrage de la création automatique des budgets récurrents pour le mois en cours");
        
        try {
            YearMonth currentMonth = YearMonth.now();
            LocalDate firstDayOfMonth = currentMonth.atDay(1);
            LocalDate lastDayOfMonth = currentMonth.atEndOfMonth();

            // Récupérer tous les budgets récurrents (templates)
            List<Budget> recurringBudgets = budgetRepository.findByIsRecurringTrueOrderByIdDesc();

            // Filtrer les budgets avec catégorie et collecter les données pour batch fetch
            List<Budget> validTemplates = new ArrayList<>();
            List<Long> userIds = new ArrayList<>();
            List<Long> categoryIds = new ArrayList<>();
            
            for (Budget templateBudget : recurringBudgets) {
                Category category = templateBudget.getCategory();
                
                // Ignorer les budgets sans catégorie
                if (category == null) {
                    logger.warn("⚠️ Budget récurrent (ID: {}) ignoré car sans catégorie. Seuls les budgets par catégorie sont supportés.", 
                        templateBudget.getId());
                    continue;
                }
                
                validTemplates.add(templateBudget);
                userIds.add(templateBudget.getUser().getId());
                categoryIds.add(category.getId());
            }

            // OPTIMISATION : Batch fetch pour vérifier l'existence de tous les budgets en une seule requête
            Set<String> existingKeys = Set.of();
            if (!userIds.isEmpty() && !categoryIds.isEmpty()) {
                List<Budget> existingBudgets = budgetRepository.findExistingBudgetsForMonth(
                    userIds, categoryIds, firstDayOfMonth, lastDayOfMonth
                );
                existingKeys = existingBudgets.stream()
                    .map(b -> b.getUser().getId() + ":" + b.getCategory().getId())
                    .collect(Collectors.toSet());
            }

            // OPTIMISATION : Collecter tous les budgets à créer pour batch insert
            List<Budget> budgetsToCreate = new ArrayList<>();
            List<NotificationRequest> notificationsToCreate = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (Budget templateBudget : validTemplates) {
                Long userId = templateBudget.getUser().getId();
                Category category = templateBudget.getCategory();
                String key = userId + ":" + category.getId();

                // Vérifier si un budget pour ce mois existe déjà (en mémoire)
                if (existingKeys.contains(key)) {
                    logger.debug("⏭️ Budget récurrent existe déjà pour utilisateur {} et catégorie {}", 
                        userId, category.getId());
                    continue;
                }

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
                
                // Préparer la notification
                notificationsToCreate.add(new NotificationRequest(
                    userId,
                    "Budget récurrent créé",
                    String.format("Un budget récurrent de %.2f MAD a été créé automatiquement pour le mois de %s.", 
                        templateBudget.getAmount(), currentMonth),
                    category.getName()
                ));
            }

            // OPTIMISATION : Batch insert de tous les budgets en une seule requête
            if (!budgetsToCreate.isEmpty()) {
                budgetRepository.saveAll(budgetsToCreate);
                logger.info("✅ {} budgets récurrents créés en batch", budgetsToCreate.size());
                
                // Créer les notifications (déjà asynchrone dans NotificationService)
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
            // Ne pas bloquer la création du budget si la notification échoue
        }
    }

}

