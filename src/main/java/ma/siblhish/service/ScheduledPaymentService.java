package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.siblhish.dto.ExpenseRequestDto;
import ma.siblhish.dto.ScheduledPaymentDto;
import ma.siblhish.dto.ScheduledPaymentRequestDto;
import ma.siblhish.entities.Category;
import ma.siblhish.entities.ScheduledPayment;
import ma.siblhish.entities.User;
import ma.siblhish.enums.TypeNotification;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.CategoryRepository;
import ma.siblhish.repository.ScheduledPaymentRepository;
import ma.siblhish.repository.UserRepository;
import ma.siblhish.service.NotificationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledPaymentService {

    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EntityMapper mapper;
    private final ExpenseService expenseService;
    private final NotificationService notificationService;

    public List<ScheduledPaymentDto> getScheduledPaymentsByUser(Long userId) {
        List<ScheduledPayment> payments = scheduledPaymentRepository.findByUserId(userId);
        return payments.stream().map(mapper::toScheduledPaymentDto).toList();
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
        payment.setRecurrenceDaysOfWeek(request.getRecurrenceDaysOfWeek() != null
                ? new ArrayList<>(request.getRecurrenceDaysOfWeek())
                : null);
        payment.setRecurrenceDayOfMonth(request.getRecurrenceDayOfMonth());
        payment.setRecurrenceDayOfYear(request.getRecurrenceDayOfYear());
        payment.setNotificationOption(request.getNotificationOption());
        payment.setIsPaid(false);
        payment.setUser(user);
        payment.setCategory(category);
        LocalDateTime now = LocalDateTime.now();
        payment.setCreationDate(now);

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

        payment.setName(request.getName());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setBeneficiary(request.getBeneficiary());
        payment.setDueDate(request.getDueDate());
        payment.setIsRecurring(request.getIsRecurring());
        payment.setRecurrenceFrequency(request.getRecurrenceFrequency());
        payment.setRecurrenceEndDate(request.getRecurrenceEndDate());
        payment.setRecurrenceDaysOfWeek(request.getRecurrenceDaysOfWeek() != null
                ? new ArrayList<>(request.getRecurrenceDaysOfWeek())
                : null);
        payment.setRecurrenceDayOfMonth(request.getRecurrenceDayOfMonth());
        payment.setRecurrenceDayOfYear(request.getRecurrenceDayOfYear());
        payment.setNotificationOption(request.getNotificationOption());

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

        ScheduledPayment saved = scheduledPaymentRepository.save(payment);
        
        // Créer une notification de confirmation de manière asynchrone (ne bloque pas la réponse)
        createPaymentMarkedAsPaidNotificationAsync(saved);
        
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
            // Nouvelle liste pour éviter le partage de référence (erreur Hibernate)
            expenseRequest.setRecurrenceDaysOfWeek(payment.getRecurrenceDaysOfWeek() != null
                    ? new ArrayList<>(payment.getRecurrenceDaysOfWeek())
                    : null);
            expenseRequest.setRecurrenceDayOfMonth(payment.getRecurrenceDayOfMonth());
            expenseRequest.setRecurrenceDayOfYear(payment.getRecurrenceDayOfYear());
        } else {
            expenseRequest.setIsRecurring(false);
        }

        // Créer la dépense via ExpenseService
        expenseService.createExpense(expenseRequest);
    }

    @Transactional
    public void deleteScheduledPayment(Long paymentId) {
        ScheduledPayment payment = scheduledPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Scheduled payment not found with id: " + paymentId));
        
        // Empêcher la suppression d'un paiement déjà payé
        if (Boolean.TRUE.equals(payment.getIsPaid())) {
            throw new RuntimeException("Un paiement planifié déjà payé ne peut pas être supprimé");
        }
        
        payment.setDeleted(true);
        scheduledPaymentRepository.save(payment);
    }
    
    /**
     * Crée une notification lorsqu'un paiement planifié est marqué comme payé (asynchrone)
     */
    @Async
    public void createPaymentMarkedAsPaidNotificationAsync(ScheduledPayment payment) {
        createPaymentMarkedAsPaidNotification(payment);
    }

    /**
     * Crée une notification lorsqu'un paiement planifié est marqué comme payé
     */
    private void createPaymentMarkedAsPaidNotification(ScheduledPayment payment) {
        try {
            String title = "✅ Paiement confirmé";
            StringBuilder description = new StringBuilder();
            description.append("Votre paiement planifié ");
            description.append(payment.getName());
            if (payment.getCategory() != null) {
                String catName = payment.getCategory().getName();
                String catIcon = payment.getCategory().getIcon();
                description.append(" catégorie ");
                description.append(catName).append("");
                if (catIcon != null && !catIcon.isBlank()) {
                    description.append(catIcon).append(" ");
                }
            }
            description.append("d'un montant de ");
            description.append(String.format("%.2f", payment.getAmount()));
            description.append(" MAD a été marqué comme payé");
            
            if (payment.getPaidDate() != null) {
                description.append(" le ");
                description.append(payment.getPaidDate().toLocalDate().toString());
                description.append(" à ");
                description.append(payment.getPaidDate().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
            }
            
            if (payment.getBeneficiary() != null && !payment.getBeneficiary().trim().isEmpty()) {
                description.append(" - Bénéficiaire : ").append(payment.getBeneficiary());
            }
            
            notificationService.createNotification(
                payment.getUser().getId(),
                title,
                description.toString(),
                TypeNotification.PAYMENT_MARKED_AS_PAID,
                "PAYMENT"
            );
            
            log.debug("📬 Notification créée pour le paiement marqué comme payé (ID: {})", payment.getId());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de la notification pour le paiement ID: {}", 
                    payment.getId(), e);
            // Ne pas bloquer le processus si la notification échoue
        }
    }
}

