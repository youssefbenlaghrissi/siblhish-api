package ma.siblhish.controller;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.ApiResponse;
import ma.siblhish.scheduler.RecurringTransactionScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Controller pour tester et déclencher manuellement les transactions récurrentes
 */
@RestController
@RequestMapping("/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionScheduler recurringTransactionScheduler;

    /**
     * Déclencher manuellement le batch de génération des transactions récurrentes
     * Utile pour tester sans attendre le scheduler automatique (03:38)
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<String>> generateRecurringTransactions() {
        try {
            recurringTransactionScheduler.generateRecurringTransactionsForDate(LocalDateTime.now());
            return ResponseEntity.ok(ApiResponse.success("Batch exécuté. Vérifiez les logs pour voir combien de transactions ont été générées."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }
}

