package ma.siblhish.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUpdateDto {
    @NotBlank(message = "Name is required")
    private String name;

    private String icon;
    private String color;
}

