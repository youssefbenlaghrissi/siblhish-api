package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.HomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour l'onglet Accueil
 * Gère le solde, les transactions récentes et l'ajout rapide de revenus/dépenses
 */
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * Obtenir le solde actuel de l'utilisateur
     */
    @GetMapping("/balance/{userId}")
    public ResponseEntity<ApiResponse<BalanceDto>> getBalance(@PathVariable Long userId) {
        BalanceDto balance = homeService.getBalance(userId);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    /**
     * Obtenir les transactions récentes avec filtres
     */
    @GetMapping("/transactions/{userId}")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getRecentTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String type, // "income", "expense", null (tous)
            @RequestParam(required = false) String dateRange, // "3days", "week", "month", "custom"
            @RequestParam(required = false) String startDate, // Format ISO pour "custom"
            @RequestParam(required = false) String endDate, // Format ISO pour "custom"
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount) {
        List<TransactionDto> transactions = homeService.getRecentTransactions(
                userId, limit, type, dateRange, startDate, endDate, minAmount, maxAmount);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    /**
     * Ajouter rapidement une dépense
     */
    @PostMapping("/expenses/quick")
    public ResponseEntity<ApiResponse<ExpenseDto>> addQuickExpense(
            @Valid @RequestBody QuickExpenseDto request) {
        ExpenseDto expense = homeService.addQuickExpense(request);
        return ResponseEntity.ok(ApiResponse.success(expense));
    }

    /**
     * Ajouter rapidement un revenu
     */
    @PostMapping("/incomes/quick")
    public ResponseEntity<ApiResponse<IncomeDto>> addQuickIncome(
            @Valid @RequestBody QuickIncomeDto request) {
        IncomeDto income = homeService.addQuickIncome(request);
        return ResponseEntity.ok(ApiResponse.success(income));
    }
}

