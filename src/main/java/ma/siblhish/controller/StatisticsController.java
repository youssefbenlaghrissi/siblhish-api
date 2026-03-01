package ma.siblhish.controller;

import jakarta.validation.constraints.NotNull;
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
     * Statistiques pour la vue mensuelle (1er au dernier jour du mois).
     *
     * @param userId ID de l'utilisateur
     * @param startDate 1er du mois (YYYY-MM-DD)
     * @param endDate Dernier jour du mois (YYYY-MM-DD)
     * @return monthlySummary (par jour), categoryExpenses, budgetStatistics
     */
    @GetMapping("/all-statistics/{userId}")
    public ResponseEntity<ApiResponse<StatisticsDto>> getAllStatistics(
            @PathVariable Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        StatisticsDto data = statisticsService.getAllStatistics(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

}

