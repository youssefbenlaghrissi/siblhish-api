package ma.siblhish.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller pour l'onglet Statistiques
 * Gère les statistiques, graphiques et analyses
 */
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Répartition des dépenses par catégorie
     * @param userId ID de l'utilisateur
     * @param startDate Date de début (format: YYYY-MM-DD)
     * @param endDate Date de fin (format: YYYY-MM-DD)
     */
    @GetMapping("/expenses-by-category/{userId}")
    public ResponseEntity<ApiResponse<StatisticsDto>> getExpensesByCategory(
            @PathVariable Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // Validation : startDate doit être <= endDate
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La date de début doit être antérieure ou égale à la date de fin"));
        }
        StatisticsDto statistics = statisticsService.getExpensesByCategory(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * Obtenir les données pour le graphique en barres (revenus vs dépenses par période)
     * @param userId ID de l'utilisateur
     * @param startDate Date de début (format: YYYY-MM-DD)
     * @param endDate Date de fin (format: YYYY-MM-DD)
     */
    @GetMapping("/expense-and-income-by-period/{userId}")
    public ResponseEntity<ApiResponse<List<PeriodSummaryDto>>> getPeriodSummary(
            @PathVariable Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // Validation : startDate doit être <= endDate
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La date de début doit être antérieure ou égale à la date de fin"));
        }
        List<PeriodSummaryDto> data = statisticsService.getPeriodSummary(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Budget vs Réel : Compare le budget prévu avec les dépenses réelles par catégorie
     * @param userId ID de l'utilisateur
     * @param startDate Date de début (format: YYYY-MM-DD)
     * @param endDate Date de fin (format: YYYY-MM-DD)
     */
    @GetMapping("/budget-vs-actual/{userId}")
    public ResponseEntity<ApiResponse<List<BudgetVsActualDto>>> getBudgetVsActual(
            @PathVariable Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La date de début doit être antérieure ou égale à la date de fin"));
        }
        List<BudgetVsActualDto> data = statisticsService.getBudgetVsActual(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Top Catégories Budgétisées : Liste les catégories avec les budgets les plus importants
     * @param userId ID de l'utilisateur
     * @param startDate Date de début (format: YYYY-MM-DD)
     * @param endDate Date de fin (format: YYYY-MM-DD)
     */
    @GetMapping("/top-budget-categories/{userId}")
    public ResponseEntity<ApiResponse<List<TopBudgetCategoryDto>>> getTopBudgetCategories(
            @PathVariable Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La date de début doit être antérieure ou égale à la date de fin"));
        }
        List<TopBudgetCategoryDto> data = statisticsService.getTopBudgetCategories(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Efficacité Budgétaire : Mesure globale de l'efficacité des budgets
     * @param userId ID de l'utilisateur
     * @param startDate Date de début (format: YYYY-MM-DD)
     * @param endDate Date de fin (format: YYYY-MM-DD)
     */
    @GetMapping("/budget-efficiency/{userId}")
    public ResponseEntity<ApiResponse<BudgetEfficiencyDto>> getBudgetEfficiency(
            @PathVariable Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La date de début doit être antérieure ou égale à la date de fin"));
        }
        BudgetEfficiencyDto data = statisticsService.getBudgetEfficiency(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Tendance Mensuelle Budgets : Évolution des budgets sur plusieurs mois
     * @param userId ID de l'utilisateur
     * @param startDate Date de début (format: YYYY-MM-DD)
     * @param endDate Date de fin (format: YYYY-MM-DD)
     */
    @GetMapping("/monthly-budget-trend/{userId}")
    public ResponseEntity<ApiResponse<List<MonthlyBudgetTrendDto>>> getMonthlyBudgetTrend(
            @PathVariable Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La date de début doit être antérieure ou égale à la date de fin"));
        }
        List<MonthlyBudgetTrendDto> data = statisticsService.getMonthlyBudgetTrend(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Répartition des Budgets : Répartition du budget total par catégorie (pour pie chart)
     * @param userId ID de l'utilisateur
     * @param startDate Date de début (format: YYYY-MM-DD)
     * @param endDate Date de fin (format: YYYY-MM-DD)
     */
    @GetMapping("/budget-distribution/{userId}")
    public ResponseEntity<ApiResponse<List<BudgetDistributionDto>>> getBudgetDistribution(
            @PathVariable Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La date de début doit être antérieure ou égale à la date de fin"));
        }
        List<BudgetDistributionDto> data = statisticsService.getBudgetDistribution(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

}

