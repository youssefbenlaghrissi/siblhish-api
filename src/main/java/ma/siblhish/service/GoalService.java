package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Category;
import ma.siblhish.entities.Goal;
import ma.siblhish.entities.User;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.CategoryRepository;
import ma.siblhish.repository.GoalRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EntityMapper mapper;

    public List<GoalDto> getGoalsByUserId(Long userId) {
        List<Goal> goals = goalRepository.findByUserIdOrderByIdDesc(userId);
        return mapper.toGoalDtoList(goals);
    }

    @Transactional
    public GoalDto createGoal(GoalRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));
        
        Goal goal = new Goal();
        goal.setName(request.getName());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(0.0);
        goal.setTargetDate(request.getTargetDate());
        goal.setIsAchieved(false);
        goal.setUser(user);
        LocalDateTime now = LocalDateTime.now();
        goal.setCreationDate(now);
        
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
            goal.setCategory(category);
        }
        
        Goal saved = goalRepository.save(goal);
        return mapper.toGoalDto(saved);
    }

    @Transactional
    public GoalDto updateGoal(Long goalId, GoalRequestDto request) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + goalId));
        
        goal.setName(request.getName());
        goal.setDescription(request.getDescription());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setCurrentAmount(request.getCurrentAmount());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
            goal.setCategory(category);
        } else {
            goal.setCategory(null);
        }
        
        // Check if goal is achieved
        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            goal.setIsAchieved(true);
        }
        
        Goal saved = goalRepository.save(goal);
        return mapper.toGoalDto(saved);
    }

    @Transactional
    public GoalDto addAmountToGoal(Long goalId, AddAmountDto request) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + goalId));
        
        goal.setCurrentAmount(goal.getCurrentAmount() + request.getAmount());
        
        // Check if goal is achieved
        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            goal.setIsAchieved(true);
        }
        
        Goal saved = goalRepository.save(goal);
        return mapper.toGoalDto(saved);
    }

    @Transactional
    public GoalDto achieveGoal(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + goalId));
        
        goal.setIsAchieved(true);
        Goal saved = goalRepository.save(goal);
        return mapper.toGoalDto(saved);
    }

    @Transactional
    public void deleteGoal(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + goalId));
        goal.setDeleted(true);
        goalRepository.save(goal);
    }

}

