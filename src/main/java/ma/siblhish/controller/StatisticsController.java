package ma.siblhish.controller;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     */
    @GetMapping("/expenses-by-category/{userId}")
    public ResponseEntity<ApiResponse<StatisticsDto>> getExpensesByCategory(
            @PathVariable Long userId,
            @RequestParam(required = false) String period) {
        StatisticsDto statistics = statisticsService.getExpensesByCategory(userId, period);
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
        List<PeriodSummaryDto> data = statisticsService.getPeriodSummary(userId, period);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

}

