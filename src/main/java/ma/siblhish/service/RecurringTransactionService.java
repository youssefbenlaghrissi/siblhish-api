package ma.siblhish.service;

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
public class RecurringTransactionService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final NotificationService notificationService;

    /**
     * Traitement par lot qui s'exécute chaque jour à 2h du matin
     * Génère automatiquement les transactions récurrentes
     */
    @Scheduled(cron = "0 35 1 * * ?") // Tous les jours à 2h du matin
    @Transactional
    public void generateRecurringTransactions() {
        generateRecurringTransactionsForDate(LocalDateTime.now());
    }

    /**
     * Méthode publique pour générer les transactions récurrentes pour une date spécifique
     * Utile pour les tests ou déclenchement manuel
     */
    @Transactional
    public void generateRecurringTransactionsForDate(LocalDateTime targetDate) {
        log.info("🔄 Début du traitement par lot pour les transactions récurrentes - Date: {}", targetDate);
        
        LocalDateTime today = targetDate;
        LocalDate todayDate = targetDate.toLocalDate();
        
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
                        todayDate)) {
                    
                    if (!transactionExists(template.getUser().getId(), template.getAmount(), 
                            template.getMethod(), today, true)) {
                        Expense created = createRecurringExpense(template, today);
                        // Créer une notification pour l'utilisateur
                        StringBuilder descBuilder = new StringBuilder("Une dépense récurrente de ");
                        descBuilder.append(String.format("%.2f", template.getAmount()));
                        descBuilder.append(" MAD a été créée automatiquement.");
                        createRecurringTransactionNotification(
                            template.getUser().getId(),
                            "Dépense récurrente créée",
                            descBuilder.toString(),
                            created.getCategory() != null ? created.getCategory().getName() : "Dépense",
                            "EXPENSE"
                        );
                        expensesGenerated++;
                    }
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
                        todayDate)) {
                    
                    if (!transactionExists(template.getUser().getId(), template.getAmount(), 
                            template.getMethod(), today, false)) {
                        Income created = createRecurringIncome(template, today);
                        // Créer une notification pour l'utilisateur
                        StringBuilder descBuilder = new StringBuilder("Un revenu récurrent de ");
                        descBuilder.append(String.format("%.2f", template.getAmount()));
                        descBuilder.append(" MAD a été créé automatiquement.");
                        createRecurringTransactionNotification(
                            template.getUser().getId(),
                            "Revenu récurrent créé",
                            descBuilder.toString(),
                            template.getSource() != null ? template.getSource() : "Revenu",
                            "INCOME"
                        );
                        incomesGenerated++;
                    }
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
                                             LocalDateTime originalDate,
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
                    int originalDayOfWeek = originalDate.getDayOfWeek().getValue();
                    return today.getDayOfWeek().getValue() == originalDayOfWeek;
                }
                int todayDayOfWeek = today.getDayOfWeek().getValue();
                return daysOfWeek.contains(todayDayOfWeek);
                
            case MONTHLY:
                // Mensuel : générer si c'est le même jour du mois
                if (dayOfMonth != null) {
                    return today.getDayOfMonth() == dayOfMonth;
                }
                // Sinon, utiliser le jour de la date originale
                return today.getDayOfMonth() == originalDate.getDayOfMonth();
                
            case YEARLY:
                // Annuel : générer si c'est le même jour de l'année
                if (dayOfYear != null) {
                    LocalDate originalLocalDate = originalDate.toLocalDate();
                    LocalDate targetDate = LocalDate.of(today.getYear(), 1, 1)
                            .plusDays(dayOfYear - 1);
                    return today.equals(targetDate);
                }
                // Sinon, utiliser le mois et jour de la date originale
                return today.getMonth() == originalDate.getMonth() 
                        && today.getDayOfMonth() == originalDate.getDayOfMonth();
                
            default:
                return false;
        }
    }

    /**
     * Vérifie si une transaction similaire existe déjà pour cette date
     */
    private boolean transactionExists(Long userId, Double amount, 
                                     ma.siblhish.enums.PaymentMethod method,
                                     LocalDateTime date, boolean isExpense) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date.toLocalDate().atTime(23, 59, 59);
        
        if (isExpense) {
            // Vérifier s'il existe une dépense avec les mêmes caractéristiques pour cette date
            List<Expense> existing = expenseRepository.findAll().stream()
                    .filter(e -> e.getUser().getId().equals(userId))
                    .filter(e -> e.getCreationDate().isAfter(startOfDay.minusSeconds(1)) 
                            && e.getCreationDate().isBefore(endOfDay.plusSeconds(1)))
                    .filter(e -> e.getAmount().equals(amount))
                    .filter(e -> e.getMethod().equals(method))
                    .filter(e -> !e.getIsRecurring()) // Ne pas compter les templates récurrents
                    .toList();
            return !existing.isEmpty();
        } else {
            // Vérifier s'il existe un revenu avec les mêmes caractéristiques pour cette date
            List<Income> existing = incomeRepository.findAll().stream()
                    .filter(i -> i.getUser().getId().equals(userId))
                    .filter(i -> i.getCreationDate().isAfter(startOfDay.minusSeconds(1)) 
                            && i.getCreationDate().isBefore(endOfDay.plusSeconds(1)))
                    .filter(i -> i.getAmount().equals(amount))
                    .filter(i -> i.getMethod().equals(method))
                    .filter(i -> !i.getIsRecurring()) // Ne pas compter les templates récurrents
                    .toList();
            return !existing.isEmpty();
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
        newExpense.setIsRecurring(false); // La transaction générée n'est pas récurrente
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
     * Crée une notification pour une transaction récurrente créée automatiquement
     */
    private void createRecurringTransactionNotification(Long userId, String title, 
                                                       String description, String categoryName, 
                                                       String transactionType) {
        try {
            notificationService.createNotification(
                userId,
                title,
                description + (categoryName != null ? " (" + categoryName + ")" : ""),
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

