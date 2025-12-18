package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.ScheduledPaymentDto;
import ma.siblhish.dto.ScheduledPaymentRequestDto;
import ma.siblhish.entities.Category;
import ma.siblhish.entities.ScheduledPayment;
import ma.siblhish.entities.User;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.CategoryRepository;
import ma.siblhish.repository.ScheduledPaymentRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledPaymentService {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EntityMapper mapper;

    public List<ScheduledPaymentDto> getScheduledPaymentsByUser(Long userId) {
        List<ScheduledPayment> payments = scheduledPaymentRepository.findByUserId(userId);
        return payments.stream().map(mapper::toScheduledPaymentDto).toList();
    }

    public List<ScheduledPaymentDto> getUnpaidPaymentsByUser(Long userId) {
        List<ScheduledPayment> payments = scheduledPaymentRepository.findUnpaidByUserId(userId);
        return payments.stream().map(mapper::toScheduledPaymentDto).toList();
    }

    public ScheduledPaymentDto getScheduledPaymentById(Long paymentId) {
        ScheduledPayment payment = scheduledPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Scheduled payment not found with id: " + paymentId));
        return mapper.toScheduledPaymentDto(payment);
    }

    @Transactional
    public ScheduledPaymentDto createScheduledPayment(ScheduledPaymentRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        ScheduledPayment payment = new ScheduledPayment();
        payment.setName(request.getName());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setBeneficiary(request.getBeneficiary());
        payment.setDueDate(request.getDueDate());
        payment.setIsRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false);
        payment.setRecurrenceFrequency(request.getRecurrenceFrequency());
        payment.setNotificationOption(request.getNotificationOption());
        payment.setIsPaid(false);
        payment.setUser(user);
        payment.setCategory(category);
        LocalDateTime now = java.time.LocalDateTime.now();
        payment.setCreationDate(now);
        payment.setUpdateDate(now);

        ScheduledPayment saved = scheduledPaymentRepository.save(payment);
        return mapper.toScheduledPaymentDto(saved);
    }

    @Transactional
    public ScheduledPaymentDto updateScheduledPayment(Long paymentId, ScheduledPaymentRequestDto request) {
        ScheduledPayment payment = scheduledPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Scheduled payment not found with id: " + paymentId));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
            payment.setCategory(category);
        }

        if (request.getName() != null) payment.setName(request.getName());
        if (request.getAmount() != null) payment.setAmount(request.getAmount());
        if (request.getPaymentMethod() != null) payment.setPaymentMethod(request.getPaymentMethod());
        if (request.getBeneficiary() != null) payment.setBeneficiary(request.getBeneficiary());
        if (request.getDueDate() != null) payment.setDueDate(request.getDueDate());
        if (request.getIsRecurring() != null) payment.setIsRecurring(request.getIsRecurring());
        if (request.getRecurrenceFrequency() != null) payment.setRecurrenceFrequency(request.getRecurrenceFrequency());
        if (request.getNotificationOption() != null) payment.setNotificationOption(request.getNotificationOption());
        payment.setUpdateDate(java.time.LocalDateTime.now());

        ScheduledPayment saved = scheduledPaymentRepository.save(payment);
        return mapper.toScheduledPaymentDto(saved);
    }

    @Transactional
    public ScheduledPaymentDto markAsPaid(Long paymentId) {
        ScheduledPayment payment = scheduledPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Scheduled payment not found with id: " + paymentId));

        payment.setIsPaid(true);
        payment.setPaidDate(LocalDateTime.now());

        // Si récurrent, créer le prochain paiement
        if (Boolean.TRUE.equals(payment.getIsRecurring()) && payment.getRecurrenceFrequency() != null) {
            createNextRecurringPayment(payment);
        }

        ScheduledPayment saved = scheduledPaymentRepository.save(payment);
        return mapper.toScheduledPaymentDto(saved);
    }

    private void createNextRecurringPayment(ScheduledPayment payment) {
        ScheduledPayment nextPayment = new ScheduledPayment();
        nextPayment.setName(payment.getName());
        nextPayment.setAmount(payment.getAmount());
        nextPayment.setPaymentMethod(payment.getPaymentMethod());
        nextPayment.setBeneficiary(payment.getBeneficiary());
        nextPayment.setIsRecurring(true);
        nextPayment.setRecurrenceFrequency(payment.getRecurrenceFrequency());
        nextPayment.setNotificationOption(payment.getNotificationOption());
        nextPayment.setIsPaid(false);
        nextPayment.setUser(payment.getUser());
        nextPayment.setCategory(payment.getCategory());
        LocalDateTime now = java.time.LocalDateTime.now();
        nextPayment.setCreationDate(now);
        nextPayment.setUpdateDate(now);

        LocalDateTime nextDueDate = switch (payment.getRecurrenceFrequency()) {
            case DAILY -> payment.getDueDate().plusDays(1);
            case WEEKLY -> payment.getDueDate().plusWeeks(1);
            case MONTHLY -> payment.getDueDate().plusMonths(1);
            case YEARLY -> payment.getDueDate().plusYears(1);
        };
        nextPayment.setDueDate(nextDueDate);

        scheduledPaymentRepository.save(nextPayment);
    }

    @Transactional
    public void deleteScheduledPayment(Long paymentId) {
        ScheduledPayment payment = scheduledPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Scheduled payment not found with id: " + paymentId));
        scheduledPaymentRepository.delete(payment);
    }
}

