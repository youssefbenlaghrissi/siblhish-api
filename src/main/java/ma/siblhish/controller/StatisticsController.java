package ma.siblhish.controller;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
     */
    @GetMapping("/expenses-by-category/{userId}")
    public ResponseEntity<ApiResponse<StatisticsDto>> getExpensesByCategory(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String period) {
        StatisticsDto statistics = statisticsService.getExpensesByCategory(userId, startDate, endDate, period);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * Évolution mensuelle des revenus et dépenses
     */
    @GetMapping("/monthly-evolution/{userId}")
    public ResponseEntity<ApiResponse<MonthlyEvolutionDto>> getMonthlyEvolution(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "6") Integer months) {
        MonthlyEvolutionDto evolution = statisticsService.getMonthlyEvolution(userId, months);
        return ResponseEntity.ok(ApiResponse.success(evolution));
    }

    /**
     * Statistiques détaillées
     */
    @GetMapping("/detailed/{userId}")
    public ResponseEntity<ApiResponse<DetailedStatisticsDto>> getDetailedStatistics(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        DetailedStatisticsDto statistics = statisticsService.getDetailedStatistics(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}

