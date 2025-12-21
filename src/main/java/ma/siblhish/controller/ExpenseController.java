package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller pour la gestion des dépenses
 */
@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    /**
     * Liste des dépenses par utilisateur (triées par date desc)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<java.util.List<ExpenseDto>>> getExpensesByUser(@PathVariable Long userId) {
        java.util.List<ExpenseDto> expenses = expenseService.getExpensesByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    /**
     * Créer une dépense
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseDto>> createExpense(
            @Valid @RequestBody ExpenseRequestDto request) {
        ExpenseDto expense = expenseService.createExpense(request);
        return ResponseEntity.status(201).body(ApiResponse.success(expense));
    }

    /**
     * Mettre à jour une dépense
     */
    @PutMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseDto>> updateExpense(
            @PathVariable Long expenseId,
            @Valid @RequestBody ExpenseRequestDto request) {
        ExpenseDto expense = expenseService.updateExpense(expenseId, request);
        return ResponseEntity.ok(ApiResponse.success(expense));
    }

    /**
     * Supprimer une dépense
     */
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable Long expenseId) {
        expenseService.deleteExpense(expenseId);
        return ResponseEntity.noContent().build();
    }

}

