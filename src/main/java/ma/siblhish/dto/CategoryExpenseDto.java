package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryExpenseDto {
    private Long categoryId;
    private String categoryName;
    private Double amount;
    private Double percentage;
    private String icon;
    private String color;
}

