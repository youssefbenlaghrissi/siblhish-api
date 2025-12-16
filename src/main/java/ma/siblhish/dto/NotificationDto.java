package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.siblhish.enums.TypeNotification;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String title;
    private String description;
    private Boolean isRead;
    private TypeNotification type;
    private LocalDateTime creationDate;
}

