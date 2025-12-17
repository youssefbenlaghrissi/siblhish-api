package ma.siblhish.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.siblhish.enums.UserType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDto {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private UserType type;
    private String language;
    private Double monthlySalary;
    private Boolean notificationsEnabled;
}

