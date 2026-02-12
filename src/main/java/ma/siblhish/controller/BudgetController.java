package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.BudgetService;
import ma.siblhish.service.BudgetSuggestionService;
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
    private final BudgetSuggestionService budgetSuggestionService;

    /**
     * Liste des budgets de l'utilisateur
     * @param month Format: YYYY-MM (ex: 2025-12). Optionnel
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<BudgetDto>>> getBudgets(
            @PathVariable Long userId,
            @RequestParam(required = false) String month) {
        List<BudgetDto> budgets = budgetService.getBudgets(userId, month);
        return ResponseEntity.ok(ApiResponse.success(budgets));
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

    /**
     * Suggérer des budgets basés sur le revenu, la situation et les catégories
     */
    @PostMapping("/suggest")
    public ResponseEntity<ApiResponse<BudgetSuggestionResponse>> suggestBudgets(
            @Valid @RequestBody BudgetSuggestionRequest request) {
        try {
            BudgetSuggestionResponse response = budgetSuggestionService.suggestBudgets(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors du calcul des budgets suggérés: " + e.getMessage()));
        }
    }

    /**
     * Créer plusieurs budgets en une seule transaction
     * Si une erreur survient, tous les budgets sont annulés (rollback)
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<BudgetDto>>> createBudgets(
            @Valid @RequestBody CreateBudgetsRequestDto request) {
        try {
            List<BudgetDto> createdBudgets = budgetService.createBudgets(request.getBudgets());
            return ResponseEntity.status(201).body(ApiResponse.success(createdBudgets));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur de validation: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la création des budgets: " + e.getMessage()));
        }
    }

}

