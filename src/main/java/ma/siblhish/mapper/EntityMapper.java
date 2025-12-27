package ma.siblhish.mapper;

import ma.siblhish.dto.*;
import ma.siblhish.entities.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

    // Budget Mappers
    public BudgetDto toBudgetDto(Budget budget, Double spent) {
        if (budget == null) return null;
        BudgetDto dto = new BudgetDto();
        dto.setId(budget.getId());
        dto.setUserId(budget.getUser().getId());
        dto.setAmount(budget.getAmount());
        dto.setStartDate(budget.getStartDate());
        dto.setEndDate(budget.getEndDate());
        dto.setCategory(toCategoryDto(budget.getCategory()));
        dto.setSpent(spent != null ? spent : 0.0);
        dto.setRemaining(budget.getAmount() - (spent != null ? spent : 0.0));
        dto.setPercentageUsed(budget.getAmount() > 0 ? 
                ((spent != null ? spent : 0.0) / budget.getAmount()) * 100 : 0.0);
        // Utiliser isRecurring depuis l'entité (stocké en base de données)
        dto.setIsRecurring(budget.getIsRecurring() != null ? budget.getIsRecurring() : false);
        return dto;
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
                user.getLanguage(),
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
        dto.setRecurrenceEndDate(payment.getRecurrenceEndDate());
        dto.setRecurrenceDaysOfWeek(payment.getRecurrenceDaysOfWeek());
        dto.setRecurrenceDayOfMonth(payment.getRecurrenceDayOfMonth());
        dto.setRecurrenceDayOfYear(payment.getRecurrenceDayOfYear());
        dto.setNotificationOption(payment.getNotificationOption());
        dto.setIsPaid(payment.getIsPaid());
        dto.setPaidDate(payment.getPaidDate());
        dto.setUserId(payment.getUser().getId());
        dto.setCategoryId(payment.getCategory().getId());
        dto.setCategory(toCategoryDto(payment.getCategory()));
        return dto;
    }

    // Card Mappers
    public CardDto toCardDto(Card card) {
        if (card == null) return null;
        return new CardDto(
                card.getId(),
                card.getCode(),
                card.getTitle()
        );
    }

    public List<CardDto> toCardDtoList(List<Card> cards) {
        return cards.stream().map(this::toCardDto).collect(Collectors.toList());
    }

    // Favorite Mappers
    public FavoriteDto toFavoriteDto(Favorite favorite) {
        if (favorite == null) return null;
        return new FavoriteDto(
                favorite.getId(),
                favorite.getUser() != null ? favorite.getUser().getId() : null,
                favorite.getType(),
                favorite.getTargetEntity(),
                favorite.getValue()
        );
    }

    public List<FavoriteDto> toFavoriteDtoList(List<Favorite> favorites) {
        return favorites.stream().map(this::toFavoriteDto).collect(Collectors.toList());
    }

    // Transaction DTO from SQL Result Row (Object[])
    // OPTIMISATION : Retourne CategoryDto imbriqué au lieu de champs séparés
    public TransactionDto toTransactionDtoFromRow(Object[] row) {
        Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
        String type = (String) row[1];
        Double amount = row[2] != null ? ((Number) row[2]).doubleValue() : null;
        String method = (String) row[3];
        String source = (String) row[4];
        String location = (String) row[5];
        String description = (String) row[6];
        LocalDateTime date = row[7] != null ? (java.time.LocalDateTime) row[7] : null;
        
        // Créer CategoryDto depuis les colonnes (category_id, category_name, category_icon, category_color)
        CategoryDto category = null;
        Long categoryId = row[8] != null ? ((Number) row[8]).longValue() : null;
        String categoryName = (String) row[9];
        String categoryIcon = (String) row[10];
        String categoryColor = (String) row[11];
        
        // Créer CategoryDto seulement si on a au moins un nom de catégorie (pour les expenses)
        if (categoryName != null && !categoryName.isEmpty()) {
            category = new CategoryDto(categoryId, categoryName, categoryIcon, categoryColor);
        }
        
        return new TransactionDto(
                id,
                type,
                amount,
                method,
                source,
                location,
                description,
                date,
                category
        );
    }

    // Utility method to convert numeric values to double
    public double convertToDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) value).doubleValue();
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
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

