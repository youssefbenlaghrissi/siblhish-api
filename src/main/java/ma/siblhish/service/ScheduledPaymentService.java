package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.ExpenseRequestDto;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledPaymentService {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EntityMapper mapper;
    private final ExpenseService expenseService;

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
        payment.setRecurrenceEndDate(request.getRecurrenceEndDate());
        // Créer une nouvelle liste pour éviter le partage de référence (erreur Hibernate)
        if (request.getRecurrenceDaysOfWeek() != null) {
            payment.setRecurrenceDaysOfWeek(new ArrayList<>(request.getRecurrenceDaysOfWeek()));
        }
        payment.setRecurrenceDayOfMonth(request.getRecurrenceDayOfMonth());
        payment.setRecurrenceDayOfYear(request.getRecurrenceDayOfYear());
        payment.setNotificationOption(request.getNotificationOption());
        payment.setIsPaid(false);
        payment.setUser(user);
        payment.setCategory(category);
        LocalDateTime now = LocalDateTime.now();
        payment.setCreationDate(now);
        payment.setUpdateDate(now);

        ScheduledPayment saved = scheduledPaymentRepository.save(payment);
        return mapper.toScheduledPaymentDto(saved);
    }

    @Transactional
    public ScheduledPaymentDto updateScheduledPayment(Long paymentId, ScheduledPaymentRequestDto request) {
        ScheduledPayment payment = scheduledPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Scheduled payment not found with id: " + paymentId));

        // Empêcher la modification d'un paiement déjà payé
        if (Boolean.TRUE.equals(payment.getIsPaid())) {
            throw new RuntimeException("Un paiement planifié déjà payé ne peut pas être modifié");
        }

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
        if (request.getRecurrenceEndDate() != null) payment.setRecurrenceEndDate(request.getRecurrenceEndDate());
        // Créer une nouvelle liste pour éviter le partage de référence (erreur Hibernate)
        if (request.getRecurrenceDaysOfWeek() != null) {
            payment.setRecurrenceDaysOfWeek(new ArrayList<>(request.getRecurrenceDaysOfWeek()));
        }
        if (request.getRecurrenceDayOfMonth() != null) payment.setRecurrenceDayOfMonth(request.getRecurrenceDayOfMonth());
        if (request.getRecurrenceDayOfYear() != null) payment.setRecurrenceDayOfYear(request.getRecurrenceDayOfYear());
        if (request.getNotificationOption() != null) payment.setNotificationOption(request.getNotificationOption());
        payment.setUpdateDate(LocalDateTime.now());

        ScheduledPayment saved = scheduledPaymentRepository.save(payment);
        return mapper.toScheduledPaymentDto(saved);
    }

    @Transactional
    public ScheduledPaymentDto markAsPaid(Long paymentId, String paymentDateStr) {
        ScheduledPayment payment = scheduledPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Scheduled payment not found with id: " + paymentId));

        // Parser la date de paiement fournie par le frontend
        LocalDateTime paymentDate;
        try {
            paymentDate = LocalDateTime.parse(paymentDateStr);
        } catch (Exception e) {
            throw new RuntimeException("Format de date invalide: " + paymentDateStr, e);
        }

        // Créer automatiquement une dépense correspondante au paiement planifié avec la date fournie
        createExpenseFromScheduledPayment(payment, paymentDate);

        payment.setIsPaid(true);
        payment.setPaidDate(paymentDate);

        // Si récurrent, créer le prochain paiement
        if (Boolean.TRUE.equals(payment.getIsRecurring()) && payment.getRecurrenceFrequency() != null) {
            createNextRecurringPayment(payment);
        }

        ScheduledPayment saved = scheduledPaymentRepository.save(payment);
        return mapper.toScheduledPaymentDto(saved);
    }

    /**
     * Crée une dépense à partir d'un paiement planifié confirmé
     */
    private void createExpenseFromScheduledPayment(ScheduledPayment payment, LocalDateTime paymentDate) {
        ExpenseRequestDto expenseRequest = new ExpenseRequestDto();
        expenseRequest.setUserId(payment.getUser().getId());
        expenseRequest.setAmount(payment.getAmount());
        expenseRequest.setMethod(payment.getPaymentMethod());
        expenseRequest.setDate(paymentDate); // Date de confirmation fournie par l'utilisateur
        expenseRequest.setDescription(payment.getName()); // Nom du paiement comme description
        expenseRequest.setLocation(payment.getBeneficiary()); // Bénéficiaire comme lieu
        expenseRequest.setCategoryId(payment.getCategory().getId());
        
        // Copier les informations de récurrence si le paiement planifié est récurrent
        if (Boolean.TRUE.equals(payment.getIsRecurring())) {
            expenseRequest.setIsRecurring(true);
            expenseRequest.setRecurrenceFrequency(payment.getRecurrenceFrequency());
            expenseRequest.setRecurrenceEndDate(payment.getRecurrenceEndDate());
            // Créer une nouvelle liste pour éviter le partage de référence (erreur Hibernate)
            if (payment.getRecurrenceDaysOfWeek() != null) {
                expenseRequest.setRecurrenceDaysOfWeek(new ArrayList<>(payment.getRecurrenceDaysOfWeek()));
            }
            expenseRequest.setRecurrenceDayOfMonth(payment.getRecurrenceDayOfMonth());
            expenseRequest.setRecurrenceDayOfYear(payment.getRecurrenceDayOfYear());
        } else {
            expenseRequest.setIsRecurring(false);
        }

        // Créer la dépense via ExpenseService
        expenseService.createExpense(expenseRequest);
    }

    private void createNextRecurringPayment(ScheduledPayment payment) {
        ScheduledPayment nextPayment = new ScheduledPayment();
        nextPayment.setName(payment.getName());
        nextPayment.setAmount(payment.getAmount());
        nextPayment.setPaymentMethod(payment.getPaymentMethod());
        nextPayment.setBeneficiary(payment.getBeneficiary());
        nextPayment.setIsRecurring(true);
        nextPayment.setRecurrenceFrequency(payment.getRecurrenceFrequency());
        nextPayment.setRecurrenceEndDate(payment.getRecurrenceEndDate());
        // Créer une nouvelle liste pour éviter le partage de référence (erreur Hibernate)
        if (payment.getRecurrenceDaysOfWeek() != null) {
            nextPayment.setRecurrenceDaysOfWeek(new ArrayList<>(payment.getRecurrenceDaysOfWeek()));
        }
        nextPayment.setRecurrenceDayOfMonth(payment.getRecurrenceDayOfMonth());
        nextPayment.setRecurrenceDayOfYear(payment.getRecurrenceDayOfYear());
        nextPayment.setNotificationOption(payment.getNotificationOption());
        nextPayment.setIsPaid(false);
        nextPayment.setUser(payment.getUser());
        nextPayment.setCategory(payment.getCategory());
        LocalDateTime now = LocalDateTime.now();
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
        
        // Empêcher la suppression d'un paiement déjà payé
        if (Boolean.TRUE.equals(payment.getIsPaid())) {
            throw new RuntimeException("Un paiement planifié déjà payé ne peut pas être supprimé");
        }
        
        scheduledPaymentRepository.delete(payment);
    }
}

