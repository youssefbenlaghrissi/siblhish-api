package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Expense;
import ma.siblhish.repository.BudgetRepository;
import ma.siblhish.repository.ExpenseRepository;
import ma.siblhish.repository.IncomeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;

    public StatisticsDto getExpensesByCategory(Long userId, LocalDate startDate, LocalDate endDate, String period) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        
        // Get all expenses in the period
        List<Expense> expenses = expenseRepository.findExpensesWithFilters(
                userId, startDateTime, endDateTime, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        
        // Group by category
        Map<Long, CategoryExpenseDto> categoryMap = new HashMap<>();
        Double total = 0.0;
        
        for (Expense expense : expenses) {
            Long categoryId = expense.getCategory().getId();
            String categoryName = expense.getCategory().getName();
            String icon = expense.getCategory().getIcon();
            String color = expense.getCategory().getColor();
            
            CategoryExpenseDto categoryExpense = categoryMap.getOrDefault(categoryId,
                    new CategoryExpenseDto(categoryId, categoryName, 0.0, 0.0, icon, color));
            
            categoryExpense.setAmount(categoryExpense.getAmount() + expense.getAmount());
            total += expense.getAmount();
            
            categoryMap.put(categoryId, categoryExpense);
        }
        
        // Calculate percentages
        List<CategoryExpenseDto> categories = new ArrayList<>(categoryMap.values());
        for (CategoryExpenseDto category : categories) {
            category.setPercentage(total > 0 ? (category.getAmount() / total) * 100 : 0.0);
        }
        
        return new StatisticsDto(period != null ? period : "custom", total, categories);
    }

    public MonthlyEvolutionDto getMonthlyEvolution(Long userId, Integer months) {
        List<MonthDataDto> evolution = new ArrayList<>();
        LocalDate now = LocalDate.now();
        
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            
            LocalDateTime startDateTime = monthStart.atStartOfDay();
            LocalDateTime endDateTime = monthEnd.atTime(23, 59, 59);
            
            Double income = incomeRepository.getTotalIncomeByUserIdAndDateRange(
                    userId, startDateTime, endDateTime);
            Double expenses = expenseRepository.getTotalExpensesByUserIdAndDateRange(
                    userId, startDateTime, endDateTime);
            
            income = income != null ? income : 0.0;
            expenses = expenses != null ? expenses : 0.0;
            
            String month = monthStart.getYear() + "-" + 
                    String.format("%02d", monthStart.getMonthValue());
            
            evolution.add(new MonthDataDto(month, income, expenses, income - expenses));
        }
        
        return new MonthlyEvolutionDto(evolution);
    }

    public DetailedStatisticsDto getDetailedStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        
        Double totalIncome = startDateTime != null && endDateTime != null
                ? incomeRepository.getTotalIncomeByUserIdAndDateRange(userId, startDateTime, endDateTime)
                : incomeRepository.getTotalIncomeByUserId(userId);
        
        Double totalExpenses = startDateTime != null && endDateTime != null
                ? expenseRepository.getTotalExpensesByUserIdAndDateRange(userId, startDateTime, endDateTime)
                : expenseRepository.getTotalExpensesByUserId(userId);
        
        totalIncome = totalIncome != null ? totalIncome : 0.0;
        totalExpenses = totalExpenses != null ? totalExpenses : 0.0;
        
        // Calculate averages
        long days = startDate != null && endDate != null
                ? java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
                : 30; // Default to 30 days
        
        Double averageDailyExpense = totalExpenses / days;
        
        // Get top expense category
        StatisticsDto categoryStats = getExpensesByCategory(userId, startDate, endDate, null);
        CategoryExpenseDto topCategory = categoryStats.getCategories().stream()
                .max(Comparator.comparing(CategoryExpenseDto::getAmount))
                .orElse(null);
        
        // Get budget status
        List<ma.siblhish.entities.Budget> activeBudgets = budgetRepository.findByUserIdAndIsActiveTrue(userId);
        Double totalBudget = activeBudgets.stream()
                .mapToDouble(ma.siblhish.entities.Budget::getAmount)
                .sum();
        
        Double spent = totalExpenses;
        Double remaining = totalBudget - spent;
        Double percentageUsed = totalBudget > 0 ? (spent / totalBudget) * 100 : 0.0;
        
        BudgetStatusDto budgetStatus = new BudgetStatusDto(totalBudget, spent, remaining, percentageUsed);
        
        return new DetailedStatisticsDto(
                totalIncome,
                totalExpenses,
                averageDailyExpense,
                totalIncome / 12.0, // Average monthly income (approximation)
                topCategory,
                budgetStatus
        );
    }
}

