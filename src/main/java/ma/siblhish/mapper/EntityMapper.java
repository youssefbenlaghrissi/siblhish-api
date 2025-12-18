package ma.siblhish.mapper;

import ma.siblhish.dto.*;
import ma.siblhish.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EntityMapper {

    // Category Mappers
    public CategoryDto toCategoryDto(Category category) {
        if (category == null) return null;
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getIcon(),
                category.getColor()
        );
    }

    public List<CategoryDto> toCategoryDtoList(List<Category> categories) {
        return categories.stream().map(this::toCategoryDto).collect(Collectors.toList());
    }

    // Expense Mappers
    public ExpenseDto toExpenseDto(Expense expense) {
        if (expense == null) return null;
        ExpenseDto dto = new ExpenseDto();
        dto.setId(expense.getId());
        dto.setAmount(expense.getAmount());
        dto.setMethod(expense.getMethod());
        dto.setDate(expense.getCreationDate());
        dto.setDescription(expense.getDescription());
        dto.setLocation(expense.getLocation());
        dto.setIsRecurring(expense.getIsRecurring());
        dto.setRecurrenceFrequency(expense.getRecurrenceFrequency());
        dto.setRecurrenceEndDate(expense.getRecurrenceEndDate());
        dto.setRecurrenceDaysOfWeek(expense.getRecurrenceDaysOfWeek());
        dto.setRecurrenceDayOfMonth(expense.getRecurrenceDayOfMonth());
        dto.setRecurrenceDayOfYear(expense.getRecurrenceDayOfYear());
        dto.setUserId(expense.getUser().getId());
        dto.setCategory(toCategoryDto(expense.getCategory()));
        return dto;
    }

    public List<ExpenseDto> toExpenseDtoList(List<Expense> expenses) {
        return expenses.stream().map(this::toExpenseDto).collect(Collectors.toList());
    }

    // Income Mappers
    public IncomeDto toIncomeDto(Income income) {
        if (income == null) return null;
        IncomeDto dto = new IncomeDto();
        dto.setId(income.getId());
        dto.setAmount(income.getAmount());
        dto.setMethod(income.getMethod());
        dto.setDate(income.getCreationDate());
        dto.setDescription(income.getDescription());
        dto.setSource(income.getSource());
        dto.setIsRecurring(income.getIsRecurring());
        dto.setRecurrenceFrequency(income.getRecurrenceFrequency());
        dto.setRecurrenceEndDate(income.getRecurrenceEndDate());
        dto.setRecurrenceDaysOfWeek(income.getRecurrenceDaysOfWeek());
        dto.setRecurrenceDayOfMonth(income.getRecurrenceDayOfMonth());
        dto.setRecurrenceDayOfYear(income.getRecurrenceDayOfYear());
        dto.setUserId(income.getUser().getId());
        return dto;
    }

    public List<IncomeDto> toIncomeDtoList(List<Income> incomes) {
        return incomes.stream().map(this::toIncomeDto).collect(Collectors.toList());
    }

    // Transaction Mapper (combines Expense and Income)
    public TransactionDto toTransactionDto(Expense expense) {
        if (expense == null) return null;
        CategoryDto category = toCategoryDto(expense.getCategory());
        TransactionDto dto = new TransactionDto();
        dto.setId(expense.getId());
        dto.setType("expense");
        dto.setAmount(expense.getAmount());
        dto.setSource(null);
        dto.setLocation(expense.getLocation());
        dto.setCategoryName(category != null ? category.getName() : null);
        dto.setCategoryIcon(category != null ? category.getIcon() : null);
        dto.setCategoryColor(category != null ? category.getColor() : null);
        dto.setDescription(expense.getDescription());
        dto.setDate(expense.getCreationDate());
        return dto;
    }

    public TransactionDto toTransactionDto(Income income) {
        if (income == null) return null;
        TransactionDto dto = new TransactionDto();
        dto.setId(income.getId());
        dto.setType("income");
        dto.setAmount(income.getAmount());
        dto.setSource(income.getSource());
        dto.setLocation(null);
        dto.setCategoryName(null);
        dto.setCategoryIcon(null);
        dto.setCategoryColor(null);
        dto.setDescription(income.getDescription());
        dto.setDate(income.getCreationDate());
        return dto;
    }

    // Budget Mappers
    public BudgetDto toBudgetDto(Budget budget, Double spent) {
        if (budget == null) return null;
        BudgetDto dto = new BudgetDto();
        dto.setId(budget.getId());
        dto.setUserId(budget.getUser().getId());
        dto.setAmount(budget.getAmount());
        dto.setPeriod(budget.getPeriod());
        dto.setStartDate(budget.getStartDate());
        dto.setEndDate(budget.getEndDate());
        dto.setIsActive(budget.getIsActive());
        dto.setCategory(toCategoryDto(budget.getCategory()));
        dto.setSpent(spent != null ? spent : 0.0);
        dto.setRemaining(budget.getAmount() - (spent != null ? spent : 0.0));
        dto.setPercentageUsed(budget.getAmount() > 0 ? 
                ((spent != null ? spent : 0.0) / budget.getAmount()) * 100 : 0.0);
        return dto;
    }

    public List<BudgetDto> toBudgetDtoList(List<Budget> budgets) {
        return budgets.stream().map(b -> toBudgetDto(b, 0.0)).collect(Collectors.toList());
    }

    // Goal Mappers
    public GoalDto toGoalDto(Goal goal) {
        if (goal == null) return null;
        GoalDto dto = new GoalDto();
        dto.setId(goal.getId());
        dto.setName(goal.getName());
        dto.setDescription(goal.getDescription());
        dto.setTargetAmount(goal.getTargetAmount());
        dto.setCurrentAmount(goal.getCurrentAmount());
        dto.setProgress(goal.getTargetAmount() > 0 ? 
                (goal.getCurrentAmount() / goal.getTargetAmount()) * 100 : 0.0);
        dto.setTargetDate(goal.getTargetDate());
        dto.setIsAchieved(goal.getIsAchieved());
        dto.setUserId(goal.getUser().getId());
        dto.setCategory(toCategoryDto(goal.getCategory()));
        return dto;
    }

    public List<GoalDto> toGoalDtoList(List<Goal> goals) {
        return goals.stream().map(this::toGoalDto).collect(Collectors.toList());
    }

    // User Profile Mappers
    public UserProfileDto toUserProfileDto(User user) {
        if (user == null) return null;
        return new UserProfileDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getType(),
                user.getLanguage(),
                user.getMonthlySalary(),
                user.getNotificationsEnabled()
        );
    }

    // Notification Mappers
    public NotificationDto toNotificationDto(Notification notification) {
        if (notification == null) return null;
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setDescription(notification.getDescription());
        dto.setIsRead(notification.getIsRead());
        dto.setType(notification.getType());
        dto.setTransactionType(notification.getTransactionType());
        dto.setCreationDate(notification.getCreationDate());
        return dto;
    }

    public List<NotificationDto> toNotificationDtoList(List<Notification> notifications) {
        return notifications.stream().map(this::toNotificationDto).collect(Collectors.toList());
    }

    // Scheduled Payment Mappers
    public ScheduledPaymentDto toScheduledPaymentDto(ScheduledPayment payment) {
        if (payment == null) return null;
        ScheduledPaymentDto dto = new ScheduledPaymentDto();
        dto.setId(payment.getId());
        dto.setName(payment.getName());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setBeneficiary(payment.getBeneficiary());
        dto.setDueDate(payment.getDueDate());
        dto.setIsRecurring(payment.getIsRecurring());
        dto.setRecurrenceFrequency(payment.getRecurrenceFrequency());
        dto.setNotificationOption(payment.getNotificationOption());
        dto.setIsPaid(payment.getIsPaid());
        dto.setPaidDate(payment.getPaidDate());
        dto.setUserId(payment.getUser().getId());
        dto.setCategoryId(payment.getCategory().getId());
        dto.setCategory(toCategoryDto(payment.getCategory()));
        return dto;
    }

    public List<ScheduledPaymentDto> toScheduledPaymentDtoList(List<ScheduledPayment> payments) {
        return payments.stream().map(this::toScheduledPaymentDto).collect(Collectors.toList());
    }

    // Page Response Mapper
    public <T> PageResponseDto<T> toPageResponseDto(Page<T> page) {
        PageResponseDto<T> response = new PageResponseDto<>();
        response.setContent(page.getContent());
        response.setTotal(page.getTotalElements());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalPages(page.getTotalPages());
        return response;
    }
}

