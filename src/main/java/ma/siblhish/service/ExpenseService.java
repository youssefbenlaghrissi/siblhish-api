package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Budget;
import ma.siblhish.entities.Category;
import ma.siblhish.entities.Expense;
import ma.siblhish.entities.User;
import ma.siblhish.enums.TypeNotification;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.BudgetRepository;
import ma.siblhish.repository.CategoryRepository;
import ma.siblhish.repository.ExpenseRepository;
import ma.siblhish.repository.UserRepository;
import ma.siblhish.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final NotificationService notificationService;
    private final EntityMapper mapper;

    @Transactional
    public ExpenseDto createExpense(ExpenseRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
        
        Expense expense = new Expense();
        expense.setAmount(request.getAmount());
        expense.setMethod(request.getMethod());
        LocalDateTime now = LocalDateTime.now();
        expense.setCreationDate(request.getDate() != null ? request.getDate() : now);
        expense.setDescription(request.getDescription());
        expense.setLocation(request.getLocation());
        expense.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        expense.setRecurrenceFrequency(request.getRecurrenceFrequency());
        expense.setRecurrenceEndDate(request.getRecurrenceEndDate());
        // Créer une nouvelle liste pour éviter le partage de référence (erreur Hibernate)
        if (request.getRecurrenceDaysOfWeek() != null) {
            expense.setRecurrenceDaysOfWeek(new ArrayList<>(request.getRecurrenceDaysOfWeek()));
        }
        expense.setRecurrenceDayOfMonth(request.getRecurrenceDayOfMonth());
        expense.setRecurrenceDayOfYear(request.getRecurrenceDayOfYear());
        expense.setUser(user);
        expense.setCategory(category);
        
        Expense saved = expenseRepository.save(expense);
        
        // Vérifier les budgets et envoyer des notifications si nécessaire
        checkAndNotifyBudgetStatus(user.getId(), category.getId(), saved.getCreationDate(), saved.getAmount());
        
        return mapper.toExpenseDto(saved);
    }

    @Transactional
    public ExpenseDto updateExpense(Long expenseId, ExpenseRequestDto request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + expenseId));
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
        
        expense.setAmount(request.getAmount());
        expense.setMethod(request.getMethod());
        // Mettre à jour creationDate si fournie, sinon garder l'ancienne valeur
        if (request.getDate() != null) {
            expense.setCreationDate(request.getDate());
        }
        expense.setDescription(request.getDescription());
        expense.setLocation(request.getLocation());
        expense.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        expense.setRecurrenceFrequency(request.getRecurrenceFrequency());
        expense.setRecurrenceEndDate(request.getRecurrenceEndDate());
        // Créer une nouvelle liste pour éviter le partage de référence (erreur Hibernate)
        if (request.getRecurrenceDaysOfWeek() != null) {
            expense.setRecurrenceDaysOfWeek(new ArrayList<>(request.getRecurrenceDaysOfWeek()));
        }
        expense.setRecurrenceDayOfMonth(request.getRecurrenceDayOfMonth());
        expense.setRecurrenceDayOfYear(request.getRecurrenceDayOfYear());
        expense.setCategory(category);
        
        Expense saved = expenseRepository.save(expense);
        return mapper.toExpenseDto(saved);
    }

    @Transactional
    public void deleteExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + expenseId));
        expense.setDeleted(true);
        expenseRepository.save(expense);
    }

    public List<ExpenseDto> getExpensesByUser(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserIdOrderByIdDesc(userId);
        return mapper.toExpenseDtoList(expenses);
    }
    
    /**
     * Vérifie les budgets affectés par cette dépense et envoie des notifications si nécessaire
     */
    private void checkAndNotifyBudgetStatus(Long userId, Long categoryId, LocalDateTime expenseDate, Double expenseAmount) {
        try {
            LocalDate expenseLocalDate = expenseDate.toLocalDate();
            
            // Récupérer tous les budgets actifs pour cet utilisateur et cette catégorie
            List<Budget> budgets = budgetRepository.findAll().stream()
                    .filter(b -> b.getUser().getId().equals(userId))
                    .filter(b -> !Boolean.TRUE.equals(b.getDeleted()))
                    .filter(b -> {
                        // Vérifier si la dépense est dans la période du budget
                        LocalDate startDate = b.getStartDate() != null ? b.getStartDate() : LocalDate.MIN;
                        LocalDate endDate = b.getEndDate() != null ? b.getEndDate() : LocalDate.MAX;
                        return !expenseLocalDate.isBefore(startDate) && !expenseLocalDate.isAfter(endDate);
                    })
                    .filter(b -> {
                        // Vérifier si le budget correspond à la catégorie (ou est global)
                        return b.getCategory() == null || b.getCategory().getId().equals(categoryId);
                    })
                    .toList();
            
            for (Budget budget : budgets) {
                // Calculer le montant dépensé pour ce budget
                Double spent = calculateSpentForBudget(budget);
                Double percentageUsed = budget.getAmount() > 0 ? (spent / budget.getAmount()) * 100 : 0.0;
                
                // Vérifier si le budget est dépassé
                if (percentageUsed >= 100) {
                    Double exceeded = spent - budget.getAmount();
                    createBudgetExceededNotification(budget, spent, exceeded, percentageUsed);
                } 
                // Vérifier si le budget atteint 90% (warning)
                else if (percentageUsed >= 90 && percentageUsed < 100) {
                    Double remaining = budget.getAmount() - spent;
                    createBudgetWarningNotification(budget, spent, remaining, percentageUsed);
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification des budgets pour la dépense: {}", e.getMessage(), e);
            // Ne pas bloquer la création de la dépense si la vérification échoue
        }
    }
    
    /**
     * Calcule le montant dépensé pour un budget donné
     */
    private Double calculateSpentForBudget(Budget budget) {
        LocalDate startDate = budget.getStartDate() != null ? budget.getStartDate() : LocalDate.MIN;
        LocalDate endDate = budget.getEndDate() != null ? budget.getEndDate() : LocalDate.MAX;
        
        return expenseRepository.findAll().stream()
                .filter(e -> e.getUser().getId().equals(budget.getUser().getId()))
                .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
                .filter(e -> {
                    LocalDateTime expenseDate = e.getCreationDate();
                    if (expenseDate == null) return false;
                    LocalDate expenseLocalDate = expenseDate.toLocalDate();
                    return !expenseLocalDate.isBefore(startDate) && !expenseLocalDate.isAfter(endDate);
                })
                .filter(e -> {
                    // Si le budget a une catégorie, filtrer par catégorie
                    if (budget.getCategory() != null) {
                        return e.getCategory() != null && e.getCategory().getId().equals(budget.getCategory().getId());
                    }
                    // Budget global : inclure toutes les dépenses
                    return true;
                })
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0.0)
                .sum();
    }
    
    /**
     * Crée une notification lorsque le budget est dépassé
     */
    private void createBudgetExceededNotification(Budget budget, Double spent, Double exceeded, Double percentageUsed) {
        try {
            String title = "⚠️ Budget dépassé";
            StringBuilder description = new StringBuilder();
            description.append("Votre budget ");
            
            if (budget.getCategory() != null) {
                description.append("\"").append(budget.getCategory().getName()).append("\"");
            } else {
                description.append("global");
            }
            
            description.append(" a été dépassé de ");
            description.append(String.format("%.2f", exceeded));
            description.append(" MAD. ");
            description.append("Dépensé : ");
            description.append(String.format("%.2f", spent));
            description.append(" / ");
            description.append(String.format("%.2f", budget.getAmount()));
            description.append(" MAD (");
            description.append(String.format("%.1f", percentageUsed));
            description.append("%)");
            
            notificationService.createNotification(
                budget.getUser().getId(),
                title,
                description.toString(),
                TypeNotification.BUDGET_EXCEEDED,
                "BUDGET"
            );
            
            log.debug("📬 Notification BUDGET_EXCEEDED créée pour le budget ID: {}", budget.getId());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de la notification BUDGET_EXCEEDED: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Crée une notification lorsque le budget atteint 90% (warning)
     */
    private void createBudgetWarningNotification(Budget budget, Double spent, Double remaining, Double percentageUsed) {
        try {
            String title = "⚠️ Attention : Budget presque atteint";
            StringBuilder description = new StringBuilder();
            description.append("Vous avez utilisé ");
            description.append(String.format("%.1f", percentageUsed));
            description.append("% de votre budget ");
            
            if (budget.getCategory() != null) {
                description.append("\"").append(budget.getCategory().getName()).append("\"");
            } else {
                description.append("global");
            }
            
            description.append(". ");
            description.append("Dépensé : ");
            description.append(String.format("%.2f", spent));
            description.append(" / ");
            description.append(String.format("%.2f", budget.getAmount()));
            description.append(" MAD. ");
            description.append("Reste : ");
            description.append(String.format("%.2f", remaining));
            description.append(" MAD");
            
            notificationService.createNotification(
                budget.getUser().getId(),
                title,
                description.toString(),
                TypeNotification.BUDGET_WARNING,
                "BUDGET"
            );
            
            log.debug("📬 Notification BUDGET_WARNING créée pour le budget ID: {}", budget.getId());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de la notification BUDGET_WARNING: {}", e.getMessage(), e);
        }
    }
}

