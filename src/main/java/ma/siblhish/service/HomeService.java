package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.repository.ExpenseRepository;
import ma.siblhish.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;

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

    /**
     * Obtenir les transactions récentes (sans filtres - les filtres sont appliqués côté frontend)
     * Retourne directement la projection simplifiée (type, title, amount)
     */
    public List<TransactionProjection> getRecentTransactions(Long userId, Integer limit) {
        // Une seule requête SQL retourne directement la projection
        return expenseRepository.findRecentTransactionsUnion(userId, limit);
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

