package ma.siblhish.repository;

import ma.siblhish.entities.Budget;
import ma.siblhish.enums.PeriodFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    
    List<Budget> findByUserIdAndIsActiveTrue(Long userId);
    
    List<Budget> findByUserIdAndCategoryId(Long userId, Long categoryId);
    
    List<Budget> findByUserIdAndPeriod(Long userId, PeriodFrequency period);
    
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId " +
           "AND (:categoryId IS NULL OR b.category.id = :categoryId) " +
           "AND (:isActive IS NULL OR b.isActive = :isActive) " +
           "AND (:period IS NULL OR b.period = :period)")
    List<Budget> findBudgetsWithFilters(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("isActive") Boolean isActive,
            @Param("period") PeriodFrequency period);
    
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId " +
           "AND b.isActive = true " +
           "AND (b.category IS NULL OR b.category.id = :categoryId) " +
           "AND b.period = :period " +
           "AND (b.startDate IS NULL OR b.startDate <= :date) " +
           "AND (b.endDate IS NULL OR b.endDate >= :date)")
    List<Budget> findActiveBudgetsForPeriod(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("period") PeriodFrequency period,
            @Param("date") LocalDate date);
}

