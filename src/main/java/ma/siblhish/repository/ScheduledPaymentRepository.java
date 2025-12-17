package ma.siblhish.repository;

import ma.siblhish.entities.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {

    @Query("SELECT sp FROM ScheduledPayment sp WHERE sp.user.id = :userId ORDER BY sp.createdAt DESC")
    List<ScheduledPayment> findByUserId(@Param("userId") Long userId);

    @Query("SELECT sp FROM ScheduledPayment sp WHERE sp.user.id = :userId AND sp.isPaid = false ORDER BY sp.createdAt DESC")
    List<ScheduledPayment> findUnpaidByUserId(@Param("userId") Long userId);

    @Query("SELECT sp FROM ScheduledPayment sp WHERE sp.user.id = :userId AND sp.isPaid = true ORDER BY sp.createdAt DESC")
    List<ScheduledPayment> findPaidByUserId(@Param("userId") Long userId);

    @Query("SELECT sp FROM ScheduledPayment sp WHERE sp.isPaid = false AND sp.dueDate <= :date")
    List<ScheduledPayment> findOverduePayments(@Param("date") LocalDateTime date);

    @Query("SELECT sp FROM ScheduledPayment sp WHERE sp.isPaid = false AND sp.dueDate BETWEEN :startDate AND :endDate")
    List<ScheduledPayment> findUpcomingPayments(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}

