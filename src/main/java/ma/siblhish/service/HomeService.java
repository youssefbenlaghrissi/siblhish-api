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

    /**
     * Obtenir les transactions récentes (sans filtres - les filtres sont appliqués côté frontend)
     * Récupère les N dépenses et N revenus les plus récents, les combine et les trie par date.
     */
    public List<TransactionDto> getRecentTransactions(Long userId, Integer limit) {
        List<TransactionDto> transactions = new ArrayList<>();
        
        // Récupérer les N dépenses les plus récentes (multiplier par 2 pour avoir assez de données à filtrer)
        List<Expense> recentExpenses = expenseRepository.findByUserIdOrderByDateDesc(userId)
                .stream()
                .limit(limit * 2L)
                .collect(Collectors.toList());
        
        // Récupérer les N revenus les plus récents
        List<Income> recentIncomes = incomeRepository.findByUserIdOrderByDateDesc(userId)
                .stream()
                .limit(limit * 2L)
                .collect(Collectors.toList());
        
        // Convertir en DTOs
        recentExpenses.forEach(expense -> 
            transactions.add(mapper.toTransactionDto(expense))
        );
        recentIncomes.forEach(income -> 
            transactions.add(mapper.toTransactionDto(income))
        );
        
        // Trier par date (plus récent en premier)
        transactions.sort(Comparator.comparing(TransactionDto::getDate).reversed());
        
        // Retourner seulement les N premières (le frontend appliquera les filtres)
        return transactions.stream()
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

