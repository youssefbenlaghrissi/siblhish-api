package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Expense;
import ma.siblhish.entities.Income;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.ExpenseRepository;
import ma.siblhish.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final EntityMapper mapper;

    public BalanceDto getBalance(Long userId) {
        Double totalIncome = incomeRepository.getTotalIncomeByUserId(userId);
        Double totalExpenses = expenseRepository.getTotalExpensesByUserId(userId);
        
        totalIncome = totalIncome != null ? totalIncome : 0.0;
        totalExpenses = totalExpenses != null ? totalExpenses : 0.0;
        
        return new BalanceDto(
                totalIncome,
                totalExpenses,
                totalIncome - totalExpenses
        );
    }

    public List<TransactionDto> getRecentTransactions(Long userId, Integer limit, String type) {
        List<TransactionDto> transactions = new ArrayList<>();
        
        if (type == null || "expense".equalsIgnoreCase(type)) {
            List<Expense> expenses = expenseRepository.findByUserId(userId, 
                    org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
            transactions.addAll(expenses.stream()
                    .map(mapper::toTransactionDto)
                    .collect(Collectors.toList()));
        }
        
        if (type == null || "income".equalsIgnoreCase(type)) {
            List<Income> incomes = incomeRepository.findByUserId(userId,
                    org.springframework.data.domain.PageRequest.of(0, limit)).getContent();
            transactions.addAll(incomes.stream()
                    .map(mapper::toTransactionDto)
                    .collect(Collectors.toList()));
        }
        
        // Sort by date descending and limit
        return transactions.stream()
                .sorted(Comparator.comparing(TransactionDto::getDate).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExpenseDto addQuickExpense(QuickExpenseDto request) {
        ExpenseRequestDto expenseRequest = new ExpenseRequestDto();
        expenseRequest.setUserId(request.getUserId());
        expenseRequest.setAmount(request.getAmount());
        expenseRequest.setCategoryId(request.getCategoryId());
        expenseRequest.setDescription(request.getDescription());
        expenseRequest.setMethod(request.getPaymentMethod());
        expenseRequest.setDate(LocalDateTime.now());
        expenseRequest.setIsRecurring(false);
        
        return expenseService.createExpense(expenseRequest);
    }

    @Transactional
    public IncomeDto addQuickIncome(QuickIncomeDto request) {
        IncomeRequestDto incomeRequest = new IncomeRequestDto();
        incomeRequest.setUserId(request.getUserId());
        incomeRequest.setAmount(request.getAmount());
        incomeRequest.setSource(request.getSource());
        incomeRequest.setDescription(request.getDescription());
        incomeRequest.setMethod(request.getPaymentMethod());
        incomeRequest.setDate(LocalDateTime.now());
        incomeRequest.setIsRecurring(false);
        
        return incomeService.createIncome(incomeRequest);
    }
}

