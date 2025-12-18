package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les filtres de transactions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFilterDto {
    private String type; // "income", "expense", null (tous)
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double minAmount;
    private Double maxAmount;
    private Integer limit; // Nombre de transactions à retourner
}

