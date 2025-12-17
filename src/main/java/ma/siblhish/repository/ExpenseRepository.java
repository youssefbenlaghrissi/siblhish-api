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
    Page<Expense> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId " +
           "AND (:startDate IS NULL OR e.date >= :startDate) " +
           "AND (:endDate IS NULL OR e.date <= :endDate) " +
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
    
    List<Expense> findByUserIdOrderByDateDesc(Long userId);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId")
    Double getTotalExpensesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId " +
           "AND e.date >= :startDate AND e.date <= :endDate")
    Double getTotalExpensesByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}

