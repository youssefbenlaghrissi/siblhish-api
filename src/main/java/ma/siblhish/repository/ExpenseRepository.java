package ma.siblhish.repository;

import ma.siblhish.entities.Expense;
import ma.siblhish.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId " +
           "AND (:startDate IS NULL OR e.creationDate >= :startDate) " +
           "AND (:endDate IS NULL OR e.creationDate <= :endDate) " +
           "AND (:categoryId IS NULL OR e.category.id = :categoryId) " +
           "AND (:minAmount IS NULL OR e.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR e.amount <= :maxAmount) " +
           "AND (:paymentMethod IS NULL OR e.method = :paymentMethod)")
    Page<Expense> findExpensesWithFilters(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("categoryId") Long categoryId,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            Pageable pageable);
    
    List<Expense> findByUserIdAndIsRecurringTrue(Long userId);
    
    List<Expense> findByIsRecurringTrue();
    
    List<Expense> findByUserIdOrderByCreationDateDesc(Long userId);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId")
    Double getTotalExpensesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId " +
           "AND e.creationDate >= :startDate AND e.creationDate <= :endDate")
    Double getTotalExpensesByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    /**
     * Requête UNION optimisée pour récupérer les transactions récentes (expenses + incomes)
     * Inclut toutes les données nécessaires : id, type, amount, source, location, category, description, date
     * @param type Optionnel : 'expense', 'income' ou NULL (pour tous les types)
     * @param minAmount Optionnel : montant minimum pour filtrer les transactions
     * @param maxAmount Optionnel : montant maximum pour filtrer les transactions
     * @param startDate Optionnel : date de début pour filtrer par période
     * @param endDate Optionnel : date de fin pour filtrer par période
     */
    @Query(value = "SELECT " +
           "id, " +
           "type, " +
           "amount, " +
           "source, " +
           "location, " +
           "category_name, " +
           "category_icon, " +
           "category_color, " +
           "description, " +
           "date " +
           "FROM (" +
           "SELECT " +
           "e.id, " +
           "'expense' as type, " +
           "e.amount, " +
           "CAST(NULL AS VARCHAR) as source, " +
           "e.location, " +
           "c.name as category_name, " +
           "c.icon as category_icon, " +
           "c.color as category_color, " +
           "e.description, " +
           "e.creation_date as date " +
           "FROM expenses e " +
           "LEFT JOIN categories c ON e.category_id = c.id " +
           "WHERE e.user_id = :userId " +
           "AND (:type IS NULL OR :type = 'expense') " +
           "AND (:minAmount IS NULL OR e.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR e.amount <= :maxAmount) " +
           "AND (:startDate IS NULL OR e.creation_date >= :startDate) " +
           "AND (:endDate IS NULL OR e.creation_date <= :endDate) " +
           "UNION ALL " +
           "SELECT " +
           "i.id, " +
           "'income' as type, " +
           "i.amount, " +
           "i.source, " +
           "CAST(NULL AS VARCHAR) as location, " +
           "CAST(NULL AS VARCHAR) as category_name, " +
           "CAST(NULL AS VARCHAR) as category_icon, " +
           "CAST(NULL AS VARCHAR) as category_color, " +
           "i.description, " +
           "i.creation_date as date " +
           "FROM incomes i " +
           "WHERE i.user_id = :userId " +
           "AND (:type IS NULL OR :type = 'income') " +
           "AND (:minAmount IS NULL OR i.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR i.amount <= :maxAmount) " +
           "AND (:startDate IS NULL OR i.creation_date >= :startDate) " +
           "AND (:endDate IS NULL OR i.creation_date <= :endDate) " +
           ") AS transactions " +
           "ORDER BY date DESC " +
           "LIMIT :limit",
           nativeQuery = true)
    List<Object[]> findRecentTransactionsUnion(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            @Param("limit") Integer limit);
}

