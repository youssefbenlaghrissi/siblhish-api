package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Income;
import ma.siblhish.entities.User;
import ma.siblhish.enums.PaymentMethod;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.IncomeRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;
    private final EntityMapper mapper;

    public PageResponseDto<IncomeDto> getIncomes(Long userId, LocalDate startDate, LocalDate endDate,
                                                  String source, Double minAmount, Double maxAmount,
                                                  PaymentMethod paymentMethod, Integer page, Integer size, String sort) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1]) 
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
        
        Page<Income> incomes = incomeRepository.findIncomesWithFilters(
                userId, startDateTime, endDateTime, source, minAmount, maxAmount, paymentMethod, pageable);
        
        PageResponseDto<IncomeDto> response = mapper.toPageResponseDto(incomes.map(mapper::toIncomeDto));
        return response;
    }

    public IncomeDto getIncomeById(Long incomeId) {
        Income income = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new RuntimeException("Income not found with id: " + incomeId));
        return mapper.toIncomeDto(income);
    }

    @Transactional
    public IncomeDto createIncome(IncomeRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));
        
        Income income = new Income();
        income.setAmount(request.getAmount());
        income.setMethod(request.getMethod());
        income.setDate(request.getDate());
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
        income.setDate(request.getDate());
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

    public List<IncomeDto> getRecurringIncomes(Long userId) {
        List<Income> incomes = incomeRepository.findByUserIdAndIsRecurringTrue(userId);
        return mapper.toIncomeDtoList(incomes);
    }

    public List<IncomeDto> getIncomesByUser(Long userId) {
        List<Income> incomes = incomeRepository.findByUserIdOrderByDateDesc(userId);
        return mapper.toIncomeDtoList(incomes);
    }
}

