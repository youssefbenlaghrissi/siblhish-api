package ma.siblhish.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * DTO pour les transactions récentes (expenses + incomes)
 */
public class TransactionDto {

    private Long id;
    private String type; // "expense" or "income"
    private String title; // description
    private Double amount;
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
            String title,
            Double amount,
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
        this.title = title;
        this.amount = amount;
        this.source = source;
        this.location = location;
        this.categoryName = categoryName;
        this.categoryIcon = categoryIcon;
        this.categoryColor = categoryColor;
        this.description = description;
        this.date = date;
    }

    // Getters et Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
