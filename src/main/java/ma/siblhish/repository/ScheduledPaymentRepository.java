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

    /**
     * Récupère les paiements planifiés avec toutes les relations chargées en une seule requête
     * Optimisation N+1 : utilise JOIN FETCH pour charger category et recurrenceDaysOfWeek
     */
    @Query("""
            SELECT DISTINCT sp
            FROM ScheduledPayment sp
            LEFT JOIN FETCH sp.category
            LEFT JOIN FETCH sp.recurrenceDaysOfWeek
            WHERE sp.user.id = :userId
                  and sp.deleted = false
            ORDER BY sp.id DESC
    """)
    List<ScheduledPayment> findByUserId(@Param("userId") Long userId);

    /**
     * Récupère les paiements non payés avec toutes les relations chargées en une seule requête
     * Optimisation N+1 : utilise JOIN FETCH pour charger category et recurrenceDaysOfWeek
     */
    @Query("SELECT DISTINCT sp FROM ScheduledPayment sp " +
           "LEFT JOIN FETCH sp.category " +
           "LEFT JOIN FETCH sp.recurrenceDaysOfWeek " +
           "WHERE sp.user.id = :userId AND sp.isPaid = false " +
           "ORDER BY sp.id DESC")
    List<ScheduledPayment> findUnpaidByUserId(@Param("userId") Long userId);

}

