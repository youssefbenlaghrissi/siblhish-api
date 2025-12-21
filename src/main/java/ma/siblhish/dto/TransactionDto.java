package ma.siblhish.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO pour les transactions récentes (expenses + incomes)
 */
@Getter
@Setter
public class TransactionDto {

    private Long id;
    private String type; // "expense" or "income"
    private Double amount;
    private String method; // méthode de paiement (CASH, CARD, etc.)
    private String source; // pour income uniquement
    private String location; // pour expense uniquement
    private String categoryName; // pour expense uniquement
    private String categoryIcon; // pour expense uniquement
    private String categoryColor; // pour expense uniquement
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime date;

    public TransactionDto(){
    }

    // Constructeur correspondant aux colonnes SELECT de votre requête SQL
    public TransactionDto(
            Long id,
            String type,
            Double amount,
            String method,
            String source,
            String location,
            String categoryName,
            String categoryIcon,
            String categoryColor,
            String description,
            LocalDateTime date
    ) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.method = method;
        this.source = source;
        this.location = location;
        this.categoryName = categoryName;
        this.categoryIcon = categoryIcon;
        this.categoryColor = categoryColor;
        this.description = description;
        this.date = date;
    }

}
