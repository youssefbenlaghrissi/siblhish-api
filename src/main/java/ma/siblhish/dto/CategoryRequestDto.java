package ma.siblhish.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDto {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Name is required")
    private String name;

    private String icon;
    private String color;
}

