package ma.siblhish.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.siblhish.enums.PaymentMethod;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuickIncomeDto {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    private String source;
    private String description;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}

