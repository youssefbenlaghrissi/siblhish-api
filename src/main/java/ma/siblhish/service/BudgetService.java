package ma.siblhish.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Budget;
import ma.siblhish.entities.Category;
import ma.siblhish.entities.User;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.BudgetRepository;
import ma.siblhish.repository.CategoryRepository;
import ma.siblhish.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final Logger logger = LoggerFactory.getLogger(BudgetService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EntityMapper mapper;

    public List<BudgetDto> getBudgets(Long userId, String month) {
        // Si un mois est fourni et valide, appliquer le filtre mois
        if (month != null && !month.isEmpty()) {
            YearMonth yearMonth = parseMonth(month);
            if (yearMonth != null) {
                LocalDate firstDayOfMonth = yearMonth.atDay(1);
                LocalDate lastDayOfMonth = yearMonth.atEndOfMonth();
                return budgetRepository.findBudgetsWithSpentByUserAndMonth(
                        userId, firstDayOfMonth, lastDayOfMonth);
            }
        }

        // Sinon, retourner tous les budgets de l'utilisateur (sans filtre de mois)
        return budgetRepository.findBudgetsWithSpentByUser(userId);
    }

    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            logger.warn("Format de mois invalide: '{}'. Format attendu: YYYY-MM (ex: 2025-12)", month);
            return null;
        }
    }

    @Transactional
    public BudgetDto createBudget(BudgetRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));
        
        Budget budget = new Budget();
        budget.setAmount(request.getAmount());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        budget.setUser(user);
        budget.setCreationDate(LocalDateTime.now());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
            budget.setCategory(category);
        }
        
        Budget saved = budgetRepository.save(budget);
        return mapper.toBudgetDto(saved, 0.0);
    }

    @Transactional
    public BudgetDto updateBudget(Long budgetId, BudgetRequestDto request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + budgetId));
        
        budget.setAmount(request.getAmount());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        if (request.getIsRecurring() != null) {
            budget.setIsRecurring(request.getIsRecurring());
        }
        
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
            budget.setCategory(category);
        } else {
            budget.setCategory(null);
        }
        
        Budget saved = budgetRepository.save(budget);
        Double spent = calculateSpent(saved);
        return mapper.toBudgetDto(saved, spent);
    }

    @Transactional
    public void deleteBudget(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + budgetId));
        budget.setDeleted(true);
        budgetRepository.save(budget);
    }

    /**
     * Supprimer plusieurs budgets en une seule transaction
     * Si une erreur survient, tous les budgets sont annulés (rollback)
     */
    @Transactional
    public void deleteBudgets(List<Long> budgetIds) {
        if (budgetIds == null || budgetIds.isEmpty()) {
            throw new IllegalArgumentException("La liste des budgets à supprimer ne peut pas être vide");
        }

        List<Budget> budgetsToDelete = budgetRepository.findAllById(budgetIds);
        
        if (budgetsToDelete.size() != budgetIds.size()) {
            throw new RuntimeException("Certains budgets n'ont pas été trouvés");
        }

        for (Budget budget : budgetsToDelete) {
            budget.setDeleted(true);
        }
        
        budgetRepository.saveAll(budgetsToDelete);
        logger.info("Suppression de {} budgets réussie en une seule transaction", budgetsToDelete.size());
    }

    private Double calculateSpent(Budget budget) {
        LocalDate startDate = getPeriodStartDate(budget);
        LocalDate endDate = getPeriodEndDate(budget);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        // Construire la requête SQL dynamiquement pour éviter les problèmes avec les paramètres NULL
        StringBuilder sql = new StringBuilder("SELECT SUM(e.amount) FROM expenses e WHERE e.user_id = :userId AND e.deleted = false ");
        
        // Ajouter les conditions seulement si elles sont nécessaires
        sql.append("AND e.creation_date >= :startDate AND e.creation_date <= :endDate ");
        
        if (budget.getCategory() != null) {
            sql.append("AND e.category_id = :categoryId ");
        }
        
        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("userId", budget.getUser().getId());
        query.setParameter("startDate", startDateTime);
        query.setParameter("endDate", endDateTime);
        
        if (budget.getCategory() != null) {
            query.setParameter("categoryId", budget.getCategory().getId());
        }
        
        Object result = query.getSingleResult();
        if (result == null) {
            return 0.0;
        }
        
        if (result instanceof Number) {
            return ((Number) result).doubleValue();
        }
        
        return 0.0;
    }

    private LocalDate getPeriodStartDate(Budget budget) {
        if (budget.getStartDate() != null) {
            return budget.getStartDate();
        }
        // Si startDate est null, utiliser la date d'aujourd'hui
        return LocalDate.now();
    }

    private LocalDate getPeriodEndDate(Budget budget) {
        if (budget.getEndDate() != null) {
            return budget.getEndDate();
        }
        // Si endDate est null, utiliser la date d'aujourd'hui
        return LocalDate.now();
    }

    /**
     * Créer plusieurs budgets en une seule transaction
     * Si une erreur survient, tous les budgets sont annulés (rollback)
     */
    @Transactional
    public List<BudgetDto> createBudgets(List<BudgetRequestDto> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("La liste des budgets ne peut pas être vide");
        }

        List<BudgetDto> createdBudgets = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        // Valider que l'utilisateur existe
        User user = userRepository.findById(requests.getFirst().getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + requests.getFirst().getUserId()));

        for (BudgetRequestDto request : requests) {
            Budget budget = new Budget();
            budget.setAmount(request.getAmount());
            budget.setStartDate(request.getStartDate());
            budget.setEndDate(request.getEndDate());
            budget.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
            budget.setUser(user);
            budget.setCreationDate(now);

            if (request.getCategoryId() != null) {
                Category category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
                budget.setCategory(category);
            }

            Budget saved = budgetRepository.save(budget);
            createdBudgets.add(mapper.toBudgetDto(saved, 0.0));
        }

        logger.info("Création de {} budgets réussie en une seule transaction", createdBudgets.size());
        return createdBudgets;
    }
}

