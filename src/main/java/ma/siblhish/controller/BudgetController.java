package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.BudgetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour la gestion des budgets
 */
@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    /**
     * Liste des budgets de l'utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<BudgetDto>>> getBudgets(@PathVariable Long userId) {
        List<BudgetDto> budgets = budgetService.getBudgets(userId);
        return ResponseEntity.ok(ApiResponse.success(budgets));
    }

    /**
     * Obtenir un budget par ID
     */
    @GetMapping("/{budgetId}")
    public ResponseEntity<ApiResponse<BudgetDto>> getBudget(@PathVariable Long budgetId) {
        BudgetDto budget = budgetService.getBudgetById(budgetId);
        return ResponseEntity.ok(ApiResponse.success(budget));
    }

    /**
     * Créer un budget
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BudgetDto>> createBudget(
            @Valid @RequestBody BudgetRequestDto request) {
        BudgetDto budget = budgetService.createBudget(request);
        return ResponseEntity.status(201).body(ApiResponse.success(budget));
    }

    /**
     * Mettre à jour un budget
     */
    @PutMapping("/{budgetId}")
    public ResponseEntity<ApiResponse<BudgetDto>> updateBudget(
            @PathVariable Long budgetId,
            @Valid @RequestBody BudgetRequestDto request) {
        BudgetDto budget = budgetService.updateBudget(budgetId, request);
        return ResponseEntity.ok(ApiResponse.success(budget));
    }

    /**
     * Supprimer un budget
     */
    @DeleteMapping("/{budgetId}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable Long budgetId) {
        budgetService.deleteBudget(budgetId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Vérifier le statut du budget
     */
    @GetMapping("/{budgetId}/status")
    public ResponseEntity<ApiResponse<BudgetStatusResponseDto>> getBudgetStatus(@PathVariable Long budgetId) {
        BudgetStatusResponseDto status = budgetService.getBudgetStatus(budgetId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

}

