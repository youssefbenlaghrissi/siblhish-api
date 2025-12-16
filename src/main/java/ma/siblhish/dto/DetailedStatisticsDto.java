package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailedStatisticsDto {
    private Double totalIncome;
    private Double totalExpenses;
    private Double averageDailyExpense;
    private Double averageMonthlyIncome;
    private CategoryExpenseDto topExpenseCategory;
    private BudgetStatusDto budgetStatus;
}

