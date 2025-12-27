package ma.siblhish.repository;

import ma.siblhish.entities.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {

    @Query("SELECT g FROM Goal g WHERE g.user.id = :userId AND g.deleted = false ORDER BY g.id DESC")
    List<Goal> findByUserIdOrderByIdDesc(@Param("userId") Long userId);
}

