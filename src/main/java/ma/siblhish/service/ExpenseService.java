package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Category;
import ma.siblhish.entities.Expense;
import ma.siblhish.entities.User;
import ma.siblhish.enums.PaymentMethod;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.CategoryRepository;
import ma.siblhish.repository.ExpenseRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EntityMapper mapper;

    public PageResponseDto<ExpenseDto> getExpenses(Long userId, LocalDate startDate, LocalDate endDate,
                                                   Long categoryId, Double minAmount, Double maxAmount,
                                                   PaymentMethod paymentMethod, Integer page, Integer size, String sort) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1]) 
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
        
        Page<Expense> expenses = expenseRepository.findExpensesWithFilters(
                userId, startDateTime, endDateTime, categoryId, minAmount, maxAmount, paymentMethod, pageable);
        
        PageResponseDto<ExpenseDto> response = mapper.toPageResponseDto(expenses.map(mapper::toExpenseDto));
        return response;
    }

    public ExpenseDto getExpenseById(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + expenseId));
        return mapper.toExpenseDto(expense);
    }

    @Transactional
    public ExpenseDto createExpense(ExpenseRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));
        
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
        
        Expense expense = new Expense();
        expense.setAmount(request.getAmount());
        expense.setMethod(request.getMethod());
        expense.setDate(request.getDate());
        expense.setDescription(request.getDescription());
        expense.setLocation(request.getLocation());
        expense.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        expense.setRecurrenceFrequency(request.getRecurrenceFrequency());
        expense.setUser(user);
        expense.setCategory(category);
        
        Expense saved = expenseRepository.save(expense);
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
        expense.setDate(request.getDate());
        expense.setDescription(request.getDescription());
        expense.setLocation(request.getLocation());
        expense.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        expense.setRecurrenceFrequency(request.getRecurrenceFrequency());
        expense.setCategory(category);
        
        Expense saved = expenseRepository.save(expense);
        return mapper.toExpenseDto(saved);
    }

    @Transactional
    public void deleteExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found with id: " + expenseId));
        expenseRepository.delete(expense);
    }

    public List<ExpenseDto> getRecurringExpenses(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserIdAndIsRecurringTrue(userId);
        return mapper.toExpenseDtoList(expenses);
    }

    public List<ExpenseDto> getExpensesByUser(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserIdOrderByDateDesc(userId);
        return mapper.toExpenseDtoList(expenses);
    }
}

