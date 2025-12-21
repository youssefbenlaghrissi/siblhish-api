package ma.siblhish.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Expense;
import ma.siblhish.repository.BudgetRepository;
import ma.siblhish.repository.ExpenseRepository;
import ma.siblhish.repository.IncomeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Obtenir les dépenses par catégorie selon la période
     * @param period : "day" (30 derniers jours), "month" (12 derniers mois), "year" (toutes les années)
     *                Si period est fourni, startDate et endDate sont ignorés
     */
    public StatisticsDto getExpensesByCategory(Long userId, LocalDate startDate, LocalDate endDate, String period) {
        String dateCondition;
        boolean useDateParams = false;
        
        // Utiliser period si fourni (priorité)
        if (period != null && !period.isEmpty()) {
            String periodLower = period.toLowerCase();
            switch (periodLower) {
                case "day":
                    // 30 derniers jours
                    dateCondition = "e.creation_date >= CURRENT_DATE - INTERVAL '30 days'";
                    break;
                case "year":
                    // Toutes les années trouvées (pas de filtre)
                    dateCondition = "1=1";
                    break;
                case "month":
                default:
                    // 12 derniers mois
                    dateCondition = "e.creation_date >= CURRENT_DATE - INTERVAL '12 months'";
                    break;
            }
        } else if (startDate != null && endDate != null) {
            // Utiliser startDate/endDate si period n'est pas fourni
            dateCondition = "e.creation_date >= :startDate AND e.creation_date <= :endDate";
            useDateParams = true;
        } else {
            // Par défaut : 12 derniers mois
            dateCondition = "e.creation_date >= CURRENT_DATE - INTERVAL '12 months'";
        }

        String sql = """
            SELECT 
                c.id as category_id,
                c.name as category_name,
                c.icon as category_icon,
                c.color as category_color,
                COALESCE(SUM(e.amount), 0) as total_amount
            FROM categories c
            LEFT JOIN expenses e ON c.id = e.category_id AND e.user_id = :userId AND """ + " " + dateCondition + """
            GROUP BY c.id, c.name, c.icon, c.color
            HAVING COALESCE(SUM(e.amount), 0) > 0
            ORDER BY total_amount DESC
        """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        
        // Ajouter les paramètres de date si nécessaire
        if (useDateParams) {
            query.setParameter("startDate", startDate.atStartOfDay());
            query.setParameter("endDate", endDate.atTime(23, 59, 59));
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        // Calculer le total pour les pourcentages
        double totalAmount = results.stream()
                .mapToDouble(row -> convertToDouble(row[4]))
                .sum();

        List<CategoryExpenseDto> categories = new ArrayList<>();
        for (Object[] row : results) {
            double amount = convertToDouble(row[4]);
            CategoryExpenseDto dto = new CategoryExpenseDto();
            dto.setCategoryId(((Number) row[0]).longValue());
            dto.setCategoryName((String) row[1]);
            dto.setIcon((String) row[2]);
            dto.setColor((String) row[3]);
            dto.setAmount(amount);
            dto.setPercentage(totalAmount > 0 ? (amount / totalAmount) * 100 : 0);
            categories.add(dto);
        }

        return new StatisticsDto(period != null ? period : "custom", totalAmount, categories);
    }

    /**
     * Convertir une valeur numérique (BigDecimal ou Double) en double
     */
    private double convertToDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
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

