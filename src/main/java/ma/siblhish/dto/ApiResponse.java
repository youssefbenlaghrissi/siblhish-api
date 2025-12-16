package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;
    private T data;
    private String message;
    private Object errors;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, "Operation successful", null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("success", data, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", null, message, null);
    }

    public static <T> ApiResponse<T> error(String message, Object errors) {
        return new ApiResponse<>("error", null, message, errors);
    }
}

