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

    @Query("SELECT DISTINCT sp FROM ScheduledPayment sp WHERE sp.user.id = :userId ORDER BY sp.creationDate DESC")
    List<ScheduledPayment> findByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT sp FROM ScheduledPayment sp WHERE sp.user.id = :userId AND sp.isPaid = false ORDER BY sp.creationDate DESC")
    List<ScheduledPayment> findUnpaidByUserId(@Param("userId") Long userId);

}

