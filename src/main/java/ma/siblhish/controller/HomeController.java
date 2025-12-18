package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.HomeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
     * Obtenir les transactions récentes (filtres appliqués côté frontend)
     * Retourne directement le DTO avec toutes les données nécessaires
     * @param type Optionnel : 'expense', 'income' ou null (pour tous les types)
     * @param minAmount Optionnel : montant minimum pour filtrer les transactions
     * @param maxAmount Optionnel : montant maximum pour filtrer les transactions
     * @param period Optionnel : période prédéfinie ('3days', 'week', 'month'). 
     *               Si fourni, prend priorité sur startDate et endDate
     * @param startDate Optionnel : date de début pour filtrer par période (format: yyyy-MM-dd)
     *                   Sera interprétée comme le début de la journée (00:00:00)
     *                   Ignoré si period est fourni
     * @param endDate Optionnel : date de fin pour filtrer par période (format: yyyy-MM-dd)
     *                 Sera interprétée comme la fin de la journée (23:59:59)
     *                 Ignoré si period est fourni
     */
    @GetMapping("/transactions/{userId}")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getRecentTransactions(
            @PathVariable Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "100") Integer limit) {
        List<TransactionDto> transactions = homeService.getRecentTransactions(userId, type, minAmount, maxAmount, period, startDate, endDate, limit);
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

