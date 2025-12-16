package ma.siblhish.service;

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

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final EntityMapper mapper;

    public List<BudgetDto> getBudgets(Long userId, Long categoryId, Boolean isActive, PeriodFrequency period) {
        List<Budget> budgets = budgetRepository.findBudgetsWithFilters(userId, categoryId, isActive, period);
        
        return budgets.stream().map(budget -> {
            Double spent = calculateSpent(budget);
            return mapper.toBudgetDto(budget, spent);
        }).collect(Collectors.toList());
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
        budget.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        budget.setUser(user);
        
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
        if (request.getIsActive() != null) budget.setIsActive(request.getIsActive());
        
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

    @Transactional
    public BudgetDto toggleBudgetActive(Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found with id: " + budgetId));
        
        budget.setIsActive(!budget.getIsActive());
        Budget saved = budgetRepository.save(budget);
        Double spent = calculateSpent(saved);
        return mapper.toBudgetDto(saved, spent);
    }

    private Double calculateSpent(Budget budget) {
        LocalDate startDate = getPeriodStartDate(budget);
        LocalDate endDate = getPeriodEndDate(budget);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        if (budget.getCategory() != null) {
            // Budget for specific category - filter by category
            List<ma.siblhish.entities.Expense> expenses = expenseRepository.findExpensesWithFilters(
                    budget.getUser().getId(), startDateTime, endDateTime, 
                    budget.getCategory().getId(), null, null, null,
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
            return expenses.stream().mapToDouble(ma.siblhish.entities.Expense::getAmount).sum();
        } else {
            // Global budget - sum all expenses
            Double total = expenseRepository.getTotalExpensesByUserIdAndDateRange(
                    budget.getUser().getId(), startDateTime, endDateTime);
            return total != null ? total : 0.0;
        }
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

