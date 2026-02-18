package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Category;
import ma.siblhish.entities.Goal;
import ma.siblhish.entities.User;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.CategoryRepository;
import ma.siblhish.config.CacheConfig;
import ma.siblhish.repository.GoalRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final CacheManager cacheManager;

    @Cacheable(value = CacheConfig.GOALS, key = "#userId")
    public List<GoalDto> getGoalsByUserId(Long userId) {
        List<Goal> goals = goalRepository.findByUserIdOrderByIdDesc(userId);
        return mapper.toGoalDtoList(goals);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.GOALS, key = "#request.userId")
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
    @CacheEvict(value = CacheConfig.GOALS, key = "#result.userId")
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

        // Gérer isAchieved selon la logique métier
        if (Boolean.TRUE.equals(request.getIsAchieved())) {
            goal.setIsAchieved(true);
            // Si une date/heure d'atteinte est fournie dans la requête, l'utiliser
            // Sinon, si achievedDate n'existe pas déjà, utiliser maintenant
            if (request.getAchievedDate() != null) {
                goal.setAchievedDate(request.getAchievedDate());
            } else if (goal.getAchievedDate() == null) {
                goal.setAchievedDate(LocalDateTime.now());
            }
            // S'assurer que currentAmount est au moins égal à targetAmount
            if (goal.getCurrentAmount() < goal.getTargetAmount()) {
                goal.setCurrentAmount(goal.getTargetAmount());
            }
        } else {
            // Si isAchieved n'est pas fourni, vérifier automatiquement selon currentAmount
            if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
                goal.setIsAchieved(true);
                if (goal.getAchievedDate() == null) {
                    goal.setAchievedDate(LocalDateTime.now());
                }
            } else {
                // Si currentAmount < targetAmount, l'objectif ne peut pas être atteint
                goal.setIsAchieved(false);
                // Ne pas supprimer achievedDate si elle existe déjà (historique)
            }
        }
        
        Goal saved = goalRepository.save(goal);
        return mapper.toGoalDto(saved);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.GOALS, key = "#result.userId")
    public GoalDto addAmountToGoal(Long goalId, AddAmountDto request) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + goalId));

        // Calculer le montant restant nécessaire pour atteindre l'objectif
        Double remainingAmount = goal.getTargetAmount() - goal.getCurrentAmount();
        
        // Vérifier que le montant à ajouter ne dépasse pas le montant restant
        if (request.getAmount() > remainingAmount) {
            throw new IllegalArgumentException(
                String.format("Le montant à ajouter (%.2f) dépasse le montant restant nécessaire (%.2f) pour atteindre l'objectif.",
                    request.getAmount(), remainingAmount)
            );
        }

        // Ajouter le montant au montant actuel
        goal.setCurrentAmount(goal.getCurrentAmount() + request.getAmount());

        // Vérifier si l'objectif est maintenant atteint (currentAmount >= targetAmount)
        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            goal.setIsAchieved(true);
            // Si l'objectif vient d'être atteint et qu'il n'y a pas encore de date d'atteinte, l'enregistrer
            if (goal.getAchievedDate() == null) {
                goal.setAchievedDate(LocalDateTime.now());
            }
        }


        Goal saved = goalRepository.save(goal);
        return mapper.toGoalDto(saved);
    }

    @Transactional
    public void deleteGoal(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + goalId));
        
        // Vérifier que l'objectif n'est pas déjà supprimé
        if (Boolean.TRUE.equals(goal.getDeleted())) {
            throw new IllegalArgumentException("L'objectif a déjà été supprimé.");
        }
        
        Long userId = goal.getUser().getId();
        goal.setDeleted(true);
        goalRepository.save(goal);
        if (cacheManager.getCache(CacheConfig.GOALS) != null) cacheManager.getCache(CacheConfig.GOALS).evict(userId);
    }

}

