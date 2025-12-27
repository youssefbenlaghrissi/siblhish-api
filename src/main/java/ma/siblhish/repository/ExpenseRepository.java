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
           "AND e.deleted = false " +
           "AND (:startDate IS NULL OR e.creationDate >= :startDate) " +
           "AND (:endDate IS NULL OR e.creationDate <= :endDate) " +
           "AND (:categoryId IS NULL OR e.category.id = :categoryId) " +
           "AND (:minAmount IS NULL OR e.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR e.amount <= :maxAmount) " +
           "AND (:paymentMethod IS NULL OR e.method = :paymentMethod) " +
           "ORDER BY e.id DESC")
    Page<Expense> findExpensesWithFilters(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("categoryId") Long categoryId,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.isRecurring = true AND e.deleted = false ORDER BY e.id DESC")
    List<Expense> findByIsRecurringTrueOrderByIdDesc();
    
    @Query("SELECT e FROM Expense e WHERE e.user.id = :userId AND e.deleted = false ORDER BY e.id DESC")
    List<Expense> findByUserIdOrderByIdDesc(@Param("userId") Long userId);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId AND e.deleted = false")
    Double getTotalExpensesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user.id = :userId " +
           "AND e.deleted = false " +
           "AND e.creationDate >= :startDate AND e.creationDate <= :endDate")
    Double getTotalExpensesByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

}

