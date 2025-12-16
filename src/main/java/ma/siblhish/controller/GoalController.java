package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.GoalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour l'onglet Objectifs
 * Gère les objectifs d'épargne
 */
@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    /**
     * Liste des objectifs de l'utilisateur
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<GoalDto>>> getGoals(
            @PathVariable Long userId,
            @RequestParam(required = false) Boolean achieved,
            @RequestParam(required = false) Long categoryId) {
        List<GoalDto> goals = goalService.getGoals(userId, achieved, categoryId);
        return ResponseEntity.ok(ApiResponse.success(goals));
    }

    /**
     * Créer un objectif
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GoalDto>> createGoal(
            @Valid @RequestBody GoalRequestDto request) {
        GoalDto goal = goalService.createGoal(request);
        return ResponseEntity.status(201).body(ApiResponse.success(goal));
    }

    /**
     * Mettre à jour un objectif
     */
    @PutMapping("/{goalId}")
    public ResponseEntity<ApiResponse<GoalDto>> updateGoal(
            @PathVariable Long goalId,
            @Valid @RequestBody GoalRequestDto request) {
        GoalDto goal = goalService.updateGoal(goalId, request);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    /**
     * Ajouter de l'argent à un objectif
     */
    @PostMapping("/{goalId}/add-amount")
    public ResponseEntity<ApiResponse<GoalDto>> addAmountToGoal(
            @PathVariable Long goalId,
            @Valid @RequestBody AddAmountDto request) {
        GoalDto goal = goalService.addAmountToGoal(goalId, request);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    /**
     * Marquer un objectif comme atteint
     */
    @PatchMapping("/{goalId}/achieve")
    public ResponseEntity<ApiResponse<GoalDto>> achieveGoal(@PathVariable Long goalId) {
        GoalDto goal = goalService.achieveGoal(goalId);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    /**
     * Supprimer un objectif
     */
    @DeleteMapping("/{goalId}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@PathVariable Long goalId) {
        goalService.deleteGoal(goalId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtenir les conseils d'épargne
     */
    @GetMapping("/{userId}/advice")
    public ResponseEntity<ApiResponse<AdviceDto>> getAdvice(@PathVariable Long userId) {
        AdviceDto advice = goalService.getAdvice(userId);
        return ResponseEntity.ok(ApiResponse.success(advice));
    }
}

