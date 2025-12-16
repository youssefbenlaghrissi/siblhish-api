package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;
    private String type; // "income" or "expense"
    private Double amount;
    private String description;
    private LocalDateTime date;
    private CategoryDto category;
    private String source; // For income
    private String location; // For expense
}

