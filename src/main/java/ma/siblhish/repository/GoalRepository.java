package ma.siblhish.repository;

import ma.siblhish.entities.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserId(Long userId);
    
    List<Goal> findByUserIdAndIsAchieved(Long userId, Boolean isAchieved);
    
    List<Goal> findByUserIdAndCategoryId(Long userId, Long categoryId);
    
    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId " +
           "AND (:achieved IS NULL OR g.isAchieved = :achieved) " +
           "AND (:categoryId IS NULL OR g.category.id = :categoryId)")
    List<Goal> findGoalsWithFilters(
            @Param("userId") Long userId,
            @Param("achieved") Boolean achieved,
            @Param("categoryId") Long categoryId);
}

