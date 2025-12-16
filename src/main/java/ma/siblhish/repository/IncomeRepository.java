package ma.siblhish.repository;

import ma.siblhish.entities.Income;
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
public interface IncomeRepository extends JpaRepository<Income, Long> {
    Page<Income> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT i FROM Income i WHERE i.user.id = :userId " +
           "AND (:startDate IS NULL OR i.date >= :startDate) " +
           "AND (:endDate IS NULL OR i.date <= :endDate) " +
           "AND (:source IS NULL OR i.source = :source) " +
           "AND (:minAmount IS NULL OR i.amount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR i.amount <= :maxAmount) " +
           "AND (:paymentMethod IS NULL OR i.method = :paymentMethod)")
    Page<Income> findIncomesWithFilters(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("source") String source,
            @Param("minAmount") Double minAmount,
            @Param("maxAmount") Double maxAmount,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            Pageable pageable);
    
    List<Income> findByUserIdAndIsRecurringTrue(Long userId);
    
    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.id = :userId")
    Double getTotalIncomeByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(i.amount) FROM Income i WHERE i.user.id = :userId " +
           "AND i.date >= :startDate AND i.date <= :endDate")
    Double getTotalIncomeByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}

