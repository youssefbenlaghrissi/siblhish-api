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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecurringTransactionScheduler {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 46 002 * * ?")
    @Transactional
    public void generateRecurringTransactions() {
        generateRecurringTransactionsForDate();
    }

    @Transactional
    public void generateRecurringTransactionsForDate() {
        LocalDateTime targetDate = LocalDateTime.now();
        log.info("🔄 Début du traitement par lot pour les transactions récurrentes - Date: {}", targetDate);

        LocalDate today = targetDate.toLocalDate();

        // Traiter les dépenses récurrentes
        List<Expense> recurringExpenses = expenseRepository.findByIsRecurringTrueOrderByIdDesc();
        List<Expense> expensesToCreate = new ArrayList<>();
        Set<String> existingExpenseKeys = new HashSet<>();

        // Charger en une fois les clés déjà présentes pour aujourd'hui (user, category, amount)
        for (Object[] row : expenseRepository.findRecurringExpenseKeysForDate(today)) {
            Long userId = (Long) row[0];
            Long categoryId = row[1] != null ? (Long) row[1] : null;
            Double amount = (Double) row[2];
            existingExpenseKeys.add(expenseKey(userId, categoryId, amount));
        }
        
        for (Expense template : recurringExpenses) {
            try {
                if (shouldGenerateTransaction(template.getRecurrenceFrequency(),
                        template.getRecurrenceEndDate(),
                        template.getRecurrenceDaysOfWeek(),
                        template.getRecurrenceDayOfMonth(),
                        template.getRecurrenceDayOfYear(),
                        template.getCreationDate(),
                        today)) {
                    Long categoryId = template.getCategory() != null ? template.getCategory().getId() : null;
                    String key = expenseKey(template.getUser().getId(), categoryId, template.getAmount());
                    if (existingExpenseKeys.contains(key)) {
                        log.debug("⏭️ Dépense récurrente similaire déjà en BDD pour user {} - {}", template.getUser().getId(), today);
                        continue;
                    }
                    Expense newExpense = createRecurringExpense(template, targetDate);
                    expensesToCreate.add(newExpense);
                    existingExpenseKeys.add(key);
                }
            } catch (Exception e) {
                log.error("❌ Erreur lors de la génération de la dépense récurrente ID: {}", 
                        template.getId(), e);
            }
        }

        int expensesGenerated = 0;
        if (!expensesToCreate.isEmpty()) {
            List<Expense> savedExpenses = expenseRepository.saveAll(expensesToCreate);
            for (Expense created : savedExpenses) {
                String description = buildRecurringExpenseDescription(created);
                createRecurringTransactionNotification(
                        created.getUser().getId(),
                        "💸 Dépense récurrente créée",
                        description,
                        null,
                        "EXPENSE"
                );
            }
            expensesGenerated = savedExpenses.size();
        }
        
        // Traiter les revenus récurrents
        List<Income> recurringIncomes = incomeRepository.findByIsRecurringTrueOrderByIdDesc();
        List<Income> incomesToCreate = new ArrayList<>();
        Set<String> existingIncomeKeys = new HashSet<>();

        // Charger en une fois les clés déjà présentes pour aujourd'hui (user, amount)
        for (Object[] row : incomeRepository.findRecurringIncomeKeysForDate(today)) {
            Long userId = (Long) row[0];
            Double amount = (Double) row[1];
            existingIncomeKeys.add(incomeKey(userId, amount));
        }
        
        for (Income template : recurringIncomes) {
            try {
                if (shouldGenerateTransaction(template.getRecurrenceFrequency(), 
                        template.getRecurrenceEndDate(), 
                        template.getRecurrenceDaysOfWeek(),
                        template.getRecurrenceDayOfMonth(),
                        template.getRecurrenceDayOfYear(),
                        template.getCreationDate(),
                        today)) {
                    String key = incomeKey(template.getUser().getId(), template.getAmount());
                    if (existingIncomeKeys.contains(key)) {
                        log.debug("⏭️ Revenu récurrent similaire déjà en BDD pour user {} - {}", template.getUser().getId(), today);
                        continue;
                    }
                    Income newIncome = createRecurringIncome(template, targetDate);
                    incomesToCreate.add(newIncome);
                    existingIncomeKeys.add(key);
                }
            } catch (Exception e) {
                log.error("❌ Erreur lors de la génération du revenu récurrent ID: {}", 
                        template.getId(), e);
            }
        }

        int incomesGenerated = 0;
        if (!incomesToCreate.isEmpty()) {
            List<Income> savedIncomes = incomeRepository.saveAll(incomesToCreate);
            for (Income created : savedIncomes) {
                String description = buildRecurringIncomeDescription(created);
                createRecurringTransactionNotification(
                        created.getUser().getId(),
                        "💰 Revenu récurrent créé",
                        description,
                        null,
                        "INCOME"
                );
            }
            incomesGenerated = savedIncomes.size();
        }
        
        log.info("✅ Traitement terminé: {} dépenses et {} revenus générés", 
                expensesGenerated, incomesGenerated);
    }

    private String expenseKey(Long userId, Long categoryId, Double amount) {
        return userId + "|" + (categoryId != null ? categoryId : "null") + "|" + amount;
    }

    private String incomeKey(Long userId, Double amount) {
        return userId + "|" + amount;
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
        // Ne pas générer avant la date de début (date de création du template)
        if (dateCreation != null && today.isBefore(dateCreation.toLocalDate())) {
            return false;
        }
        
        if (frequency == null) {
            return false;
        }
        
        switch (frequency) {
            case DAILY:
                // Quotidien : générer chaque jour à partir de la date de création
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
        newExpense.setParentTransactionId(template.getId());
        newExpense.setUser(template.getUser());
        newExpense.setCategory(template.getCategory());
        return newExpense;
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
        newIncome.setParentTransactionId(template.getId());
        newIncome.setUser(template.getUser());
        return newIncome;
    }

    /**
     * Construit la description pour une dépense récurrente créée (même structure que ScheduledPaymentReminderScheduler).
     */
    private String buildRecurringExpenseDescription(Expense expense) {
        StringBuilder desc = new StringBuilder();
        String label = expense.getDescription() != null && !expense.getDescription().isBlank()
                ? expense.getDescription() : "Dépense";
        desc.append("Votre dépense récurrente \"").append(label).append("\"");
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
        desc.append("Votre revenu récurrent \"").append(label).append("\"");
        desc.append(" d'un montant de ");
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

