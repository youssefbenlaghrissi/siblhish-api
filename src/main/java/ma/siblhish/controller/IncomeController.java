package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.enums.PaymentMethod;
import ma.siblhish.service.IncomeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller pour la gestion des revenus
 */
@RestController
@RequestMapping("/incomes")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;

    /**
     * Liste des revenus par utilisateur (triés par date desc)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<IncomeDto>>> getIncomesByUser(@PathVariable Long userId) {
        List<IncomeDto> incomes = incomeService.getIncomesByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(incomes));
    }

    /**
     * Obtenir un revenu par ID
     */
    @GetMapping("/{incomeId}")
    public ResponseEntity<ApiResponse<IncomeDto>> getIncome(@PathVariable Long incomeId) {
        IncomeDto income = incomeService.getIncomeById(incomeId);
        return ResponseEntity.ok(ApiResponse.success(income));
    }

    /**
     * Créer un revenu
     */
    @PostMapping
    public ResponseEntity<ApiResponse<IncomeDto>> createIncome(
            @Valid @RequestBody IncomeRequestDto request) {
        IncomeDto income = incomeService.createIncome(request);
        return ResponseEntity.status(201).body(ApiResponse.success(income));
    }

    /**
     * Mettre à jour un revenu
     */
    @PutMapping("/{incomeId}")
    public ResponseEntity<ApiResponse<IncomeDto>> updateIncome(
            @PathVariable Long incomeId,
            @Valid @RequestBody IncomeRequestDto request) {
        IncomeDto income = incomeService.updateIncome(incomeId, request);
        return ResponseEntity.ok(ApiResponse.success(income));
    }

    /**
     * Supprimer un revenu
     */
    @DeleteMapping("/{incomeId}")
    public ResponseEntity<ApiResponse<Void>> deleteIncome(@PathVariable Long incomeId) {
        incomeService.deleteIncome(incomeId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtenir les revenus récurrents
     */
    @GetMapping("/{userId}/recurring")
    public ResponseEntity<ApiResponse<List<IncomeDto>>> getRecurringIncomes(@PathVariable Long userId) {
        List<IncomeDto> incomes = incomeService.getRecurringIncomes(userId);
        return ResponseEntity.ok(ApiResponse.success(incomes));
    }
}

