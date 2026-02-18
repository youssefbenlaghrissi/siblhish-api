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
import ma.siblhish.config.CacheConfig;
import ma.siblhish.repository.BudgetRepository;
import ma.siblhish.repository.CategoryRepository;
import ma.siblhish.repository.ExpenseRepository;
import ma.siblhish.repository.UserRepository;
import ma.siblhish.service.NotificationService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private final CacheManager cacheManager;

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.EXPENSES, CacheConfig.BALANCE}, key = "#request.userId")
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
        checkAndNotifyBudgetStatus(user.getId(), category.getId(), saved.getCreationDate());
        
        return mapper.toExpenseDto(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {CacheConfig.EXPENSES, CacheConfig.BALANCE}, key = "#result.userId")
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
        Long userId = expense.getUser().getId();
        expense.setDeleted(true);
        expenseRepository.save(expense);
        evictUserCaches(userId);
    }

    @Cacheable(value = CacheConfig.EXPENSES, key = "#userId")
    public List<ExpenseDto> getExpensesByUser(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserIdOrderByIdDesc(userId);
        return mapper.toExpenseDtoList(expenses);
    }

    private void evictUserCaches(Long userId) {
        if (cacheManager.getCache(CacheConfig.EXPENSES) != null) {
            cacheManager.getCache(CacheConfig.EXPENSES).evict(userId);
        }
        if (cacheManager.getCache(CacheConfig.BALANCE) != null) {
            cacheManager.getCache(CacheConfig.BALANCE).evict(userId);
        }
    }
    
    /**
     * Vérifie les budgets affectés par cette dépense et envoie des notifications si nécessaire
     */
    @Async
    public void checkAndNotifyBudgetStatus(Long userId, Long categoryId, LocalDateTime expenseDate) {
        try {
            Optional<Budget> currentBudget = budgetRepository.findCurrentBudgetByCategory(
                    userId, expenseDate.toLocalDate(), categoryId);
            if(currentBudget.isEmpty()) {
                return;
            }

            Budget budget = currentBudget.get();
            Double spent = calculateSpentForBudgetOptimized(budget);
            double percentageUsed = budget.getAmount() > 0 ? (spent / budget.getAmount()) * 100 : 0.0;
                
            // Vérifier si le budget est dépassé
            if (percentageUsed >= 100) {
                Double exceeded = spent - budget.getAmount();
                createBudgetExceededNotification(budget, spent, exceeded, percentageUsed);
            }
            // Vérifier si le budget atteint 90% (warning)
            else if (percentageUsed >= 90) {
                Double remaining = budget.getAmount() - spent;
                createBudgetWarningNotification(budget, spent, remaining, percentageUsed);
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors de la vérification des budgets pour la dépense: {}", e.getMessage(), e);
            // Ne pas bloquer la création de la dépense si la vérification échoue
        }
    }
    
    /**
     * Calcule le montant dépensé pour un budget donné
     * OPTIMISÉ : Utilise SUM() directement en SQL au lieu de charger toutes les dépenses
     */
    private Double calculateSpentForBudgetOptimized(Budget budget) {
        LocalDate startDate = budget.getStartDate() != null ? budget.getStartDate() : LocalDate.MIN;
        LocalDate endDate = budget.getEndDate() != null ? budget.getEndDate() : LocalDate.MAX;
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        Long categoryId = budget.getCategory() != null ? budget.getCategory().getId() : null;
        
        Double result = expenseRepository.calculateSpentForBudget(
                budget.getUser().getId(),
                startDateTime,
                endDateTime,
                categoryId
        );
        
        return result != null ? result : 0.0;
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

