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

    /**
     * Liste des paiements planifiés par utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ScheduledPaymentDto>>> getScheduledPaymentsByUser(@PathVariable Long userId) {
        List<ScheduledPaymentDto> payments = scheduledPaymentService.getScheduledPaymentsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    /**
     * Liste des paiements non payés par utilisateur
     */
    @GetMapping("/user/{userId}/unpaid")
    public ResponseEntity<ApiResponse<List<ScheduledPaymentDto>>> getUnpaidPaymentsByUser(@PathVariable Long userId) {
        List<ScheduledPaymentDto> payments = scheduledPaymentService.getUnpaidPaymentsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    /**
     * Obtenir un paiement planifié par ID
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<ScheduledPaymentDto>> getScheduledPayment(@PathVariable Long paymentId) {
        ScheduledPaymentDto payment = scheduledPaymentService.getScheduledPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(payment));
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
            @RequestParam(required = false) String paymentDate) {
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
}

