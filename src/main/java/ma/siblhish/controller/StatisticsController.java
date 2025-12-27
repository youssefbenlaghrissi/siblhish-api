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
     * Endpoint unifié pour récupérer TOUTES les statistiques en une seule requête
     * Optimise les performances en réduisant les appels API de 6 à 1
     * @param userId ID de l'utilisateur
     * @param startDate Date de début (format: YYYY-MM-DD)
     * @param endDate Date de fin (format: YYYY-MM-DD)
     * @return DTO unifié contenant toutes les statistiques (monthlySummary, categoryExpenses, budgetStatistics)
     */
    @GetMapping("/all-statistics/{userId}")
    public ResponseEntity<ApiResponse<StatisticsDto>> getAllStatistics(
            @PathVariable Long userId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La date de début doit être antérieure ou égale à la date de fin"));
        }
        StatisticsDto data = statisticsService.getAllStatistics(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

}

