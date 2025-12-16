package ma.siblhish.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnumsDto {
    private List<String> userTypes;
    private List<String> paymentMethods;
    private List<String> periodFrequencies;
    private List<String> recurrenceFrequencies;
    private List<String> notificationTypes;
}

