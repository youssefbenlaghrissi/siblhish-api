package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Category;
import ma.siblhish.entities.Expense;
import ma.siblhish.entities.User;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.CategoryRepository;
import ma.siblhish.repository.ExpenseRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EntityMapper mapper;

    @Transactional
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
        expense.setUpdateDate(now);
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
        // Mettre à jour creationDate si fournie, sinon garder l'ancienne valeur
        if (request.getDate() != null) {
            expense.setCreationDate(request.getDate());
        }
        expense.setUpdateDate(LocalDateTime.now());
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
        expenseRepository.delete(expense);
    }

    public List<ExpenseDto> getExpensesByUser(Long userId) {
        List<Expense> expenses = expenseRepository.findByUserIdOrderByCreationDateDesc(userId);
        return mapper.toExpenseDtoList(expenses);
    }
}

