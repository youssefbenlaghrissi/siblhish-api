package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Income;
import ma.siblhish.entities.User;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.IncomeRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;
    private final EntityMapper mapper;

    @Transactional
    public IncomeDto createIncome(IncomeRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));
        
        Income income = new Income();
        income.setAmount(request.getAmount());
        income.setMethod(request.getMethod());
        LocalDateTime now = LocalDateTime.now();
        income.setCreationDate(request.getDate() != null ? request.getDate() : now);
        income.setUpdateDate(now);
        income.setDescription(request.getDescription());
        income.setSource(request.getSource());
        income.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        income.setRecurrenceFrequency(request.getRecurrenceFrequency());
        income.setRecurrenceEndDate(request.getRecurrenceEndDate());
        income.setRecurrenceDaysOfWeek(request.getRecurrenceDaysOfWeek());
        income.setRecurrenceDayOfMonth(request.getRecurrenceDayOfMonth());
        income.setRecurrenceDayOfYear(request.getRecurrenceDayOfYear());
        income.setUser(user);
        
        Income saved = incomeRepository.save(income);
        return mapper.toIncomeDto(saved);
    }

    @Transactional
    public IncomeDto updateIncome(Long incomeId, IncomeRequestDto request) {
        Income income = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new RuntimeException("Income not found with id: " + incomeId));
        
        income.setAmount(request.getAmount());
        income.setMethod(request.getMethod());
        // Mettre à jour creationDate si fournie, sinon garder l'ancienne valeur
        if (request.getDate() != null) {
            income.setCreationDate(request.getDate());
        }
        income.setUpdateDate(LocalDateTime.now());
        income.setDescription(request.getDescription());
        income.setSource(request.getSource());
        income.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        income.setRecurrenceFrequency(request.getRecurrenceFrequency());
        income.setRecurrenceEndDate(request.getRecurrenceEndDate());
        income.setRecurrenceDaysOfWeek(request.getRecurrenceDaysOfWeek());
        income.setRecurrenceDayOfMonth(request.getRecurrenceDayOfMonth());
        income.setRecurrenceDayOfYear(request.getRecurrenceDayOfYear());
        
        Income saved = incomeRepository.save(income);
        return mapper.toIncomeDto(saved);
    }

    @Transactional
    public void deleteIncome(Long incomeId) {
        Income income = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new RuntimeException("Income not found with id: " + incomeId));
        incomeRepository.delete(income);
    }

    public List<IncomeDto> getIncomesByUser(Long userId) {
        List<Income> incomes = incomeRepository.findByUserIdOrderByIdDesc(userId);
        return mapper.toIncomeDtoList(incomes);
    }
}

