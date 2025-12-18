package ma.siblhish.dto;

/**
 * Projection simplifiée pour les transactions récentes
 * Contient uniquement : type, title (description), amount
 */
public interface TransactionProjection {
    String getType(); // "expense" or "income"
    String getTitle(); // description de la transaction
    Double getAmount();
}

