package ma.siblhish.scheduler;

import lombok.RequiredArgsConstructor;
import ma.siblhish.entities.Budget;
import ma.siblhish.entities.Category;
import ma.siblhish.repository.BudgetRepository;
import ma.siblhish.service.BudgetService;
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
            List<Budget> recurringBudgets = budgetRepository.findByIsRecurringTrue();

            for (Budget templateBudget : recurringBudgets) {
                Long userId = templateBudget.getUser().getId();
                Category category = templateBudget.getCategory();

                // Vérifier si un budget pour ce mois existe déjà
                boolean exists;
                List<Budget> existingBudgets = budgetRepository.findByUserIdAndCategoryIdAndStartDateAndEndDate(
                            userId, category.getId(), firstDayOfMonth, lastDayOfMonth
                    );
                exists = !existingBudgets.isEmpty();

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

                    budgetRepository.save(newBudget);
                }
            }
            logger.info("✅ Création automatique des budgets récurrents terminée avec succès");
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création automatique des budgets récurrents: {}", e.getMessage(), e);
        }
    }

}

