package ma.siblhish.controller;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.StatisticsGraphService;
import ma.siblhish.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller pour l'onglet Statistiques
 * Gère les statistiques, graphiques et analyses
 */
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final StatisticsGraphService statisticsGraphService;

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

    /**
     * Obtenir les données pour le graphique en barres (revenus vs dépenses par période)
     * @param period : "day" (jour), "month" (mois), "year" (année)
     */
    @GetMapping("/expense-and-income-by-period/{userId}")
    public ResponseEntity<ApiResponse<List<PeriodSummaryDto>>> getPeriodSummary(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "month") String period) {
        List<PeriodSummaryDto> data = statisticsGraphService.getPeriodSummary(userId, period);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Obtenir les préférences des cartes statistiques de l'utilisateur
     */
    @GetMapping("/cards-preferences/{userId}")
    public ResponseEntity<ApiResponse<List<String>>> getCardsPreferences(@PathVariable Long userId) {
        List<String> preferences = statisticsGraphService.getCardsPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * Mettre à jour les préférences des cartes statistiques
     */
    @PutMapping("/cards-preferences/{userId}")
    public ResponseEntity<ApiResponse<List<String>>> updateCardsPreferences(
            @PathVariable Long userId,
            @RequestBody Map<String, List<String>> request) {
        List<String> cards = request.get("cards");
        if (cards == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Le champ 'cards' est requis"));
        }
        List<String> updated = statisticsGraphService.updateCardsPreferences(userId, cards);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
}

