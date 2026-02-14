package ma.siblhish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.ApiResponse;
import ma.siblhish.dto.ScheduledPaymentDto;
import ma.siblhish.dto.ScheduledPaymentRequestDto;
import ma.siblhish.service.ScheduledPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour la gestion des paiements planifiés
 */
@RestController
@RequestMapping("/scheduled-payments")
@RequiredArgsConstructor
public class ScheduledPaymentController {

    private final ScheduledPaymentService scheduledPaymentService;
    private final ma.siblhish.scheduler.RecurringScheduledPaymentScheduler recurringScheduledPaymentScheduler;

    /**
     * Liste des paiements planifiés par utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ScheduledPaymentDto>>> getScheduledPaymentsByUser(@PathVariable Long userId) {
        List<ScheduledPaymentDto> payments = scheduledPaymentService.getScheduledPaymentsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    /**
     * Créer un paiement planifié
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledPaymentDto>> createScheduledPayment(
            @Valid @RequestBody ScheduledPaymentRequestDto request) {
        ScheduledPaymentDto payment = scheduledPaymentService.createScheduledPayment(request);
        return ResponseEntity.status(201).body(ApiResponse.success(payment));
    }

    /**
     * Mettre à jour un paiement planifié
     */
    @PutMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<ScheduledPaymentDto>> updateScheduledPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody ScheduledPaymentRequestDto request) {
        ScheduledPaymentDto payment = scheduledPaymentService.updateScheduledPayment(paymentId, request);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    /**
     * Marquer un paiement comme payé
     */
    @PutMapping("/{paymentId}/pay")
    public ResponseEntity<ApiResponse<ScheduledPaymentDto>> markAsPaid(
            @PathVariable Long paymentId,
            @RequestParam String paymentDate) {
        ScheduledPaymentDto payment = scheduledPaymentService.markAsPaid(paymentId, paymentDate);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    /**
     * Supprimer un paiement planifié
     */
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<Void>> deleteScheduledPayment(@PathVariable Long paymentId) {
        scheduledPaymentService.deleteScheduledPayment(paymentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Déclencher manuellement le batch de génération des prochains paiements planifiés récurrents
     * Utile pour tester sans attendre le scheduler automatique (04:00)
     */
    @PostMapping("/recurring/generate")
    public ResponseEntity<ApiResponse<String>> generateNextRecurringPayments() {
        try {
            recurringScheduledPaymentScheduler.createNextRecurringPaymentsInternal();
            return ResponseEntity.ok(ApiResponse.success("Batch exécuté. Vérifiez les logs pour voir combien de paiements ont été générés."));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la génération des prochains paiements récurrents: " + e.getMessage()));
        }
    }
}

