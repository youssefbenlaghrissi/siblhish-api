package ma.siblhish.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Budget;
import ma.siblhish.entities.Category;
import ma.siblhish.entities.User;
import ma.siblhish.enums.PeriodFrequency;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.BudgetRepository;
import ma.siblhish.repository.CategoryRepository;
import ma.siblhish.repository.ExpenseRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    @PersistenceContext
    private EntityManager entityManager;

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final EntityMapper mapper;

    public List<BudgetDto> getBudgets(Long userId) {
        // Requête SQL simple : récupérer les budgets avec leur spent calculé et les infos de catégorie
        String sql = """
            SELECT 
                b.id,
                b.user_id,
                b.amount,
                b.period,
                b.start_date,
                b.end_date,
                b.category_id,
                c.name as category_name,
                c.icon as category_icon,
                c.color as category_color,
                b.creation_date,
                b.update_date,
                COALESCE((
                    SELECT SUM(e.amount)
                    FROM expenses e
                    WHERE e.user_id = b.user_id
                      AND e.creation_date BETWEEN b.start_date AND b.end_date
                      AND (b.category_id IS NULL OR e.category_id = b.category_id)
                ), 0) as spent
            FROM budgets b
            LEFT JOIN categories c ON b.category_id = c.id
            WHERE b.user_id = :userId
        """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream().map(row -> {
            Budget budget = new Budget();
            budget.setId(((Number) row[0]).longValue());
            budget.setAmount(((Number) row[2]).doubleValue());
            budget.setPeriod(PeriodFrequency.valueOf((String) row[3]));
            budget.setStartDate(convertToLocalDate(row[4]));
            budget.setEndDate(convertToLocalDate(row[5]));
            if (row[6] != null) {
                Category cat = new Category();
                cat.setId(((Number) row[6]).longValue());
                cat.setName((String) row[7]);
                cat.setIcon((String) row[8]);
                cat.setColor((String) row[9]);
                budget.setCategory(cat);
            }
            budget.setCreationDate(convertToLocalDateTime(row[10]));
            budget.setUpdateDate(convertToLocalDateTime(row[11]));
            
            User user = new User();
            user.setId(((Number) row[1]).longValue());
            budget.setUser(user);
            
            Double spent = mapper.convertToDouble(row[12]);
            return mapper.toBudgetDto(budget, spent);
        }).collect(Collectors.toList());
    }
    
    private LocalDate convertToLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        return null;
    }
    
    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof java.sql.Timestamp) return ((java.sql.Timestamp) value).toLocalDateTime();
        return null;
    }

    public BudgetDto getBudgetById(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + budgetId));
        Double spent = calculateSpent(budget);
        return mapper.toBudgetDto(budget, spent);
    }

    @Transactional
    public BudgetDto createBudget(BudgetRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));
        
        Budget budget = new Budget();
        budget.setAmount(request.getAmount());
        budget.setPeriod(request.getPeriod());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setUser(user);
        LocalDateTime now = LocalDateTime.now();
        budget.setCreationDate(now);

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
        budget.setPeriod(request.getPeriod());
        budget.setStartDate(request.getStartDate());
        budget.setEndDate(request.getEndDate());
        budget.setUpdateDate(LocalDateTime.now());
        
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
        budgetRepository.delete(budget);
    }

    public BudgetStatusResponseDto getBudgetStatus(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + budgetId));
        
        Double spent = calculateSpent(budget);
        Double remaining = budget.getAmount() - spent;
        Double percentageUsed = budget.getAmount() > 0 ? (spent / budget.getAmount()) * 100 : 0.0;
        
        String status = "OK";
        String message = "Budget is within limits";
        
        if (percentageUsed >= 100) {
            status = "EXCEEDED";
            message = "Budget exceeded by " + String.format("%.2f", Math.abs(remaining)) + " MAD";
        } else if (percentageUsed >= 90) {
            status = "WARNING";
            message = "You have used " + String.format("%.1f", percentageUsed) + "% of your budget";
        }
        
        return new BudgetStatusResponseDto(
                budgetId, budget.getAmount(), spent, remaining, percentageUsed, status, message
        );
    }

    private Double calculateSpent(Budget budget) {
        LocalDate startDate = getPeriodStartDate(budget);
        LocalDate endDate = getPeriodEndDate(budget);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        // Construire la requête SQL dynamiquement pour éviter les problèmes avec les paramètres NULL
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(e.amount), 0) FROM expenses e WHERE e.user_id = :userId ");
        
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
        
        LocalDate now = LocalDate.now();
        return switch (budget.getPeriod()) {
            case DAILY -> now;
            case WEEKLY -> now.minusDays(now.getDayOfWeek().getValue() - 1);
            case MONTHLY -> now.withDayOfMonth(1);
            case YEARLY -> now.withDayOfYear(1);
        };
    }

    private LocalDate getPeriodEndDate(Budget budget) {
        if (budget.getEndDate() != null) {
            return budget.getEndDate();
        }
        
        LocalDate now = LocalDate.now();
        return switch (budget.getPeriod()) {
            case DAILY -> now;
            case WEEKLY -> now.plusDays(7 - now.getDayOfWeek().getValue());
            case MONTHLY -> now.withDayOfMonth(now.lengthOfMonth());
            case YEARLY -> now.withDayOfYear(now.lengthOfYear());
        };
    }
}

