package ma.siblhish.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddAmountDto {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;
}

