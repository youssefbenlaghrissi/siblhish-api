package ma.siblhish.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.siblhish.entities.Expense;
import ma.siblhish.entities.Income;
import ma.siblhish.enums.RecurrenceFrequency;
import ma.siblhish.enums.TypeNotification;
import ma.siblhish.repository.ExpenseRepository;
import ma.siblhish.repository.IncomeRepository;
import ma.siblhish.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionScheduler {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 50 3 * * ?")
    @Transactional
    public void generateRecurringTransactions() {
        generateRecurringTransactionsForDate();
    }

    @Transactional
    public void generateRecurringTransactionsForDate() {
        LocalDateTime targetDate = LocalDateTime.now();
        log.info("🔄 Début du traitement par lot pour les transactions récurrentes - Date: {}", targetDate);

        // Traiter les dépenses récurrentes
        List<Expense> recurringExpenses = expenseRepository.findByIsRecurringTrueOrderByIdDesc();
        int expensesGenerated = 0;
        
        for (Expense template : recurringExpenses) {
            try {
                if (shouldGenerateTransaction(template.getRecurrenceFrequency(),
                        template.getRecurrenceEndDate(),
                        template.getRecurrenceDaysOfWeek(),
                        template.getRecurrenceDayOfMonth(),
                        template.getRecurrenceDayOfYear(),
                        template.getCreationDate(),
                        targetDate.toLocalDate())) {
                    Expense created = createRecurringExpense(template, targetDate);
                    String description = buildRecurringExpenseDescription(created);
                    createRecurringTransactionNotification(
                            template.getUser().getId(),
                            "📉 Dépense récurrente créée",
                            description,
                            null,
                            "EXPENSE"
                    );
                        expensesGenerated++;
                    }
            } catch (Exception e) {
                log.error("❌ Erreur lors de la génération de la dépense récurrente ID: {}", 
                        template.getId(), e);
            }
        }
        
        // Traiter les revenus récurrents
        List<Income> recurringIncomes = incomeRepository.findByIsRecurringTrueOrderByIdDesc();
        int incomesGenerated = 0;
        
        for (Income template : recurringIncomes) {
            try {
                if (shouldGenerateTransaction(template.getRecurrenceFrequency(), 
                        template.getRecurrenceEndDate(), 
                        template.getRecurrenceDaysOfWeek(),
                        template.getRecurrenceDayOfMonth(),
                        template.getRecurrenceDayOfYear(),
                        template.getCreationDate(),
                        targetDate.toLocalDate())) {

                        Income created = createRecurringIncome(template, targetDate);
                        String description = buildRecurringIncomeDescription(created);
                        createRecurringTransactionNotification(
                                created.getUser().getId(),
                                "📈 Revenu récurrent créé",
                                description,
                                null,
                                "INCOME"
                        );
                        incomesGenerated++;
                    }
            } catch (Exception e) {
                log.error("❌ Erreur lors de la génération du revenu récurrent ID: {}", 
                        template.getId(), e);
            }
        }
        
        log.info("✅ Traitement terminé: {} dépenses et {} revenus générés", 
                expensesGenerated, incomesGenerated);
    }

    /**
     * Vérifie si une transaction doit être générée aujourd'hui
     */
    private boolean shouldGenerateTransaction(RecurrenceFrequency frequency,
                                             LocalDateTime endDate,
                                             List<Integer> daysOfWeek,
                                             Integer dayOfMonth,
                                             Integer dayOfYear,
                                             LocalDateTime dateCreation,
                                             LocalDate today) {
        
        // Vérifier la date limite
        if (endDate != null && today.isAfter(endDate.toLocalDate())) {
            return false;
        }
        
        if (frequency == null) {
            return false;
        }
        
        switch (frequency) {
            case DAILY:
                // Quotidien : générer chaque jour
                return true;
                
            case WEEKLY:
                // Hebdomadaire : générer si aujourd'hui est dans les jours sélectionnés
                if (daysOfWeek == null || daysOfWeek.isEmpty()) {
                    // Si aucun jour spécifié, utiliser le jour de la date originale
                    int originalDayOfWeek = dateCreation.getDayOfWeek().getValue();
                    return today.getDayOfWeek().getValue() == originalDayOfWeek;
                }
                int todayDayOfWeek = today.getDayOfWeek().getValue();
                return daysOfWeek.contains(todayDayOfWeek);
                
            case MONTHLY:
                // Mensuel : générer si c'est le même jour du mois
                if (dayOfMonth != null) {
                    return today.getDayOfMonth() == dayOfMonth;
                }
                return false;
            case YEARLY:
                // Annuel : générer si c'est le même jour de l'année
                if (dayOfYear != null) {
                    LocalDate targetDate = LocalDate.of(today.getYear(), 1, 1)
                            .plusDays(dayOfYear - 1);
                    return today.equals(targetDate);
                }
                // Fallback pour les anciennes données : utiliser le mois et jour de la date originale
                return today.getMonth() == dateCreation.getMonth()
                        && today.getDayOfMonth() == dateCreation.getDayOfMonth();
                
            default:
                return false;
        }
    }

    /**
     * Crée une nouvelle dépense basée sur le template récurrent
     */
    private Expense createRecurringExpense(Expense template, LocalDateTime date) {
        Expense newExpense = new Expense();
        newExpense.setAmount(template.getAmount());
        newExpense.setMethod(template.getMethod());
        newExpense.setCreationDate(date);
        newExpense.setDescription(template.getDescription());
        newExpense.setLocation(template.getLocation());
        newExpense.setIsRecurring(false);
        newExpense.setRecurrenceFrequency(null);
        newExpense.setUser(template.getUser());
        newExpense.setCategory(template.getCategory());
        
        Expense saved = expenseRepository.save(newExpense);
        log.debug("✅ Dépense récurrente créée: {} MAD pour l'utilisateur {}", 
                template.getAmount(), template.getUser().getId());
        return saved;
    }

    /**
     * Crée un nouveau revenu basé sur le template récurrent
     */
    private Income createRecurringIncome(Income template, LocalDateTime date) {
        Income newIncome = new Income();
        newIncome.setAmount(template.getAmount());
        newIncome.setMethod(template.getMethod());
        newIncome.setCreationDate(date);
        newIncome.setDescription(template.getDescription());
        newIncome.setSource(template.getSource());
        newIncome.setIsRecurring(false); // La transaction générée n'est pas récurrente
        newIncome.setRecurrenceFrequency(null);
        newIncome.setUser(template.getUser());
        
        Income saved = incomeRepository.save(newIncome);
        log.debug("✅ Revenu récurrent créé: {} MAD pour l'utilisateur {}", 
                template.getAmount(), template.getUser().getId());
        return saved;
    }

    /**
     * Construit la description pour une dépense récurrente créée (même structure que ScheduledPaymentReminderScheduler).
     */
    private String buildRecurringExpenseDescription(Expense expense) {
        StringBuilder desc = new StringBuilder();
        String label = expense.getDescription() != null && !expense.getDescription().isBlank()
                ? expense.getDescription() : "Dépense";
        desc.append("Votre dépense récurrente ").append(label).append("\"");;
        if (expense.getCategory() != null) {
            String catName = expense.getCategory().getName();
            String catIcon = expense.getCategory().getIcon();
            desc.append(" catégorie ");
            desc.append(catName);
            if (catIcon != null && !catIcon.isBlank()) {
                desc.append(" ").append(catIcon).append(" ");
            }
        }
        desc.append("d'un montant de ");
        desc.append(String.format("%.2f", expense.getAmount()));
        desc.append(" MAD, a été créée automatiquement.");
        return desc.toString();
    }

    /**
     * Construit la description pour un revenu récurrent créé (même structure que ScheduledPaymentReminderScheduler).
     */
    private String buildRecurringIncomeDescription(Income income) {
        StringBuilder desc = new StringBuilder();
        String label = income.getSource() != null && !income.getSource().isBlank()
                ? income.getSource() : (income.getDescription() != null && !income.getDescription().isBlank()
                ? income.getDescription() : "Revenu");
        desc.append("Votre revenu récurrent ").append(label).append("\"");
        desc.append(", d'un montant de ");
        desc.append(String.format("%.2f", income.getAmount()));
        desc.append(" MAD, a été créé automatiquement.");
        return desc.toString();
    }

    /**
     * Crée une notification pour une transaction récurrente créée automatiquement
     */
    private void createRecurringTransactionNotification(Long userId, String title, 
                                                       String description, String categoryName, 
                                                       String transactionType) {
        try {
            notificationService.createNotification(
                userId,
                title,
                description != null ? description : "",
                TypeNotification.RECURRING_TRANSACTION,
                transactionType
            );
            log.debug("📬 Notification créée pour l'utilisateur {} - Type: {}", userId, transactionType);
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de la notification pour l'utilisateur {}: {}", 
                    userId, e.getMessage());
            // Ne pas bloquer la création de la transaction si la notification échoue
        }
    }
}

