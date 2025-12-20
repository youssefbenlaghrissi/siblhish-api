package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteDto {
    private Long id;
    private Long userId;
    private String type;
    private Long targetEntity;
    private String value;
}

