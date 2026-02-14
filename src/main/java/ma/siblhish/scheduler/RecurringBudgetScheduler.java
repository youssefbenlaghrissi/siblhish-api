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

            for (Budget templateBudget : recurringBudgets) {
                Long userId = templateBudget.getUser().getId();
                Category category = templateBudget.getCategory();

                // Vérifier si un budget pour ce mois existe déjà
                boolean exists;
                if (category != null) {
                    List<Budget> existingBudgets = budgetRepository.findByUserIdAndCategoryIdAndStartDateAndEndDateOrderByIdDesc(
                                userId, category.getId(), firstDayOfMonth, lastDayOfMonth
                        );
                    exists = !existingBudgets.isEmpty();
                } else {
                    // Pour les budgets globaux (category null)
                    List<Budget> existingBudgets = budgetRepository.findByUserIdAndCategoryIsNullAndStartDateAndEndDateOrderByIdDesc(
                                userId, firstDayOfMonth, lastDayOfMonth
                        );
                    exists = !existingBudgets.isEmpty();
                }

                if (!exists) {
                    // Créer un nouveau budget pour ce mois avec toute la logique métier
                    Budget newBudget = new Budget();
                    newBudget.setUser(templateBudget.getUser());
                    newBudget.setAmount(templateBudget.getAmount());
                    newBudget.setStartDate(firstDayOfMonth);
                    newBudget.setEndDate(lastDayOfMonth);
                    newBudget.setIsRecurring(true);
                    newBudget.setCategory(category);
                    newBudget.setCreationDate(LocalDateTime.now());

                    Budget savedBudget = budgetRepository.save(newBudget);
                    
                    // Créer une notification pour l'utilisateur
                    createRecurringBudgetNotification(
                        userId,
                        "Budget récurrent créé",
                        String.format("Un budget récurrent de %.2f MAD a été créé automatiquement pour le mois de %s.", 
                            templateBudget.getAmount(), 
                            currentMonth.toString()),
                        category != null ? category.getName() : "Budget global"
                    );
                }
            }
            logger.info("✅ Création automatique des budgets récurrents terminée avec succès");
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création automatique des budgets récurrents: {}", e.getMessage(), e);
        }
    }

    /**
     * Crée une notification pour un budget récurrent créé automatiquement
     */
    private void createRecurringBudgetNotification(Long userId, String title, 
                                                   String description, String categoryName) {
        try {
            notificationService.createNotification(
                userId,
                title,
                description + (categoryName != null ? " (" + categoryName + ")" : ""),
                TypeNotification.RECURRING_BUDGET,
                "BUDGET"
            );
            logger.debug("📬 Notification créée pour l'utilisateur {} - Budget récurrent", userId);
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création de la notification pour l'utilisateur {}: {}", 
                    userId, e.getMessage());
            // Ne pas bloquer la création du budget si la notification échoue
        }
    }

}

