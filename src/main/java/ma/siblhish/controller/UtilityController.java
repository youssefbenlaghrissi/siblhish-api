package ma.siblhish.controller;

import ma.siblhish.dto.*;
import ma.siblhish.enums.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Controller pour les utilitaires et endpoints généraux
 */
@RestController
@RequestMapping("")
public class UtilityController {

    /**
     * Obtenir tous les enums disponibles
     */
    @GetMapping("/enums")
    public ResponseEntity<ApiResponse<EnumsDto>> getEnums() {
        EnumsDto enumsDto = new EnumsDto(
                Arrays.stream(UserType.values()).map(Enum::name).collect(Collectors.toList()),
                Arrays.stream(PaymentMethod.values()).map(Enum::name).collect(Collectors.toList()),
                Arrays.stream(PeriodFrequency.values()).map(Enum::name).collect(Collectors.toList()),
                Arrays.stream(RecurrenceFrequency.values()).map(Enum::name).collect(Collectors.toList()),
                Arrays.stream(TypeNotification.values()).map(Enum::name).collect(Collectors.toList())
        );
        return ResponseEntity.ok(ApiResponse.success(enumsDto));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthDto>> health() {
        HealthDto health = new HealthDto("UP", LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.success(health));
    }
}

