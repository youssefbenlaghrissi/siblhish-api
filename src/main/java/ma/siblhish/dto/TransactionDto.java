package ma.siblhish.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * DTO pour les transactions récentes (expenses + incomes)
 * Optimisé pour minimiser les traitements côté frontend
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
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime date;

    // Catégorie structurée (pour expense uniquement) - OPTIMISATION : objet imbriqué au lieu de champs séparés
    private CategoryDto category;

    /**
     * OPTIMISATION : Retourne le timestamp Unix en millisecondes
     * Permet au frontend d'éviter le parsing de string (DateTime.parse est coûteux)
     * Le frontend peut utiliser : DateTime.fromMillisecondsSinceEpoch(timestamp)
     * 
     * @return Timestamp Unix en millisecondes, ou null si date est null
     */
    @JsonProperty("dateTimestamp")
    public Long getDateTimestamp() {
        if (date == null) return null;
        return date.atZone(ZoneId.systemDefault())
                   .toInstant()
                   .toEpochMilli();
    }

    public TransactionDto(){
    }

    // Constructeur optimisé avec CategoryDto imbriqué
    public TransactionDto(
            Long id,
            String type,
            Double amount,
            String method,
            String source,
            String location,
            String description,
            LocalDateTime date,
            CategoryDto category
    ) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.method = method;
        this.source = source;
        this.location = location;
        this.description = description;
        this.date = date;
        this.category = category;
    }

    // Constructeur de compatibilité (pour migration progressive)
    @Deprecated
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
        this.description = description;
        this.date = date;
        // Créer CategoryDto depuis les champs séparés (pour compatibilité)
        if (categoryName != null) {
            this.category = new CategoryDto(null, categoryName, categoryIcon, categoryColor);
        }
    }

}
