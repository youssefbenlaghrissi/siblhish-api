package ma.siblhish.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.ExpenseRepository;
import ma.siblhish.repository.IncomeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    @PersistenceContext
    private EntityManager entityManager;
    
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final EntityMapper mapper;

    public BalanceDto getBalance(Long userId) {
        Double totalIncome = incomeRepository.getTotalIncomeByUserId(userId);
        Double totalExpenses = expenseRepository.getTotalExpensesByUserId(userId);
        
        totalIncome = totalIncome != null ? totalIncome : 0.0;
        totalExpenses = totalExpenses != null ? totalExpenses : 0.0;
        
        return new BalanceDto(
                totalIncome,
                totalExpenses,
                totalIncome - totalExpenses
        );
    }

    /**
     * Obtenir les transactions récentes avec filtres optionnels
     * Retourne directement le DTO avec toutes les données nécessaires
     * @param userId ID de l'utilisateur
     * @param limit Nombre maximum de transactions à retourner
     * @param type Optionnel : 'expense', 'income' ou null (pour tous les types)
     * @param dateRange Optionnel : période prédéfinie ('3days', 'week', 'month', 'custom')
     * @param startDate Optionnel : date de début pour filtrer par période (sera convertie en début de journée 00:00:00)
     *                  Requis si dateRange='custom'
     * @param endDate Optionnel : date de fin pour filtrer par période (sera convertie en fin de journée 23:59:59)
     *                Requis si dateRange='custom'
     * @param minAmount Optionnel : montant minimum pour filtrer les transactions
     * @param maxAmount Optionnel : montant maximum pour filtrer les transactions
     */
    public List<TransactionDto> getRecentTransactions(
            Long userId, 
            Integer limit, 
            String type, 
            String dateRange, 
            LocalDate startDate, 
            LocalDate endDate, 
            Double minAmount, 
            Double maxAmount) {
        LocalDate calculatedStartDate = startDate;
        LocalDate calculatedEndDate = endDate;
        
        // Si une période prédéfinie est fournie, calculer les dates
        if (dateRange != null && !dateRange.isEmpty() && !"custom".equalsIgnoreCase(dateRange)) {
            LocalDate now = LocalDate.now();
            calculatedEndDate = now; // Toujours jusqu'à aujourd'hui
            
            switch (dateRange.toLowerCase()) {
                case "3days":
                    calculatedStartDate = now.minusDays(3);
                    break;
                case "week":
                    calculatedStartDate = now.minusWeeks(1);
                    break;
                case "month":
                    calculatedStartDate = now.minusMonths(1);
                    break;
                default:
                    // Si la période n'est pas reconnue, ignorer et utiliser startDate/endDate si fournis
                    break;
            }
        }
        // Si dateRange='custom', utiliser startDate et endDate fournis directement
        
        // Convertir LocalDate en LocalDateTime pour la requête SQL
        // startDate -> début de journée (00:00:00)
        // endDate -> fin de journée (23:59:59)
        LocalDateTime startDateTime = calculatedStartDate != null ? calculatedStartDate.atStartOfDay() : null;
        LocalDateTime endDateTime = calculatedEndDate != null ? calculatedEndDate.atTime(23, 59, 59) : null;
        
        // Construire la requête SQL dynamiquement pour éviter les problèmes avec les paramètres NULL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("id, type, amount, method, source, location, description, date, ");
        sql.append("category_id, category_name, category_icon, category_color ");
        sql.append("FROM (");
        
        List<String> unionParts = new ArrayList<>();
        
        // Partie Expenses (seulement si type = null ou type = 'expense')
        if (type == null || "expense".equalsIgnoreCase(type)) {
            StringBuilder expenseQuery = new StringBuilder();
            expenseQuery.append("SELECT ");
            expenseQuery.append("e.id, 'expense' as type, ");
            expenseQuery.append("e.amount, e.payment_method as method, NULL as source, e.location, ");
            expenseQuery.append("e.description, e.creation_date as date, ");
            expenseQuery.append("c.id as category_id, c.name as category_name, c.icon as category_icon, c.color as category_color ");
            expenseQuery.append("FROM expenses e ");
            expenseQuery.append("LEFT JOIN categories c ON e.category_id = c.id ");
            expenseQuery.append("WHERE e.user_id = :userId ");
            
            List<String> expenseConditions = new ArrayList<>();
            if (minAmount != null) {
                expenseConditions.add("e.amount >= :minAmount");
            }
            if (maxAmount != null) {
                expenseConditions.add("e.amount <= :maxAmount");
            }
            if (startDateTime != null) {
                expenseConditions.add("e.creation_date >= :startDate");
            }
            if (endDateTime != null) {
                expenseConditions.add("e.creation_date <= :endDate");
            }
            
            if (!expenseConditions.isEmpty()) {
                expenseQuery.append("AND ");
                for (int i = 0; i < expenseConditions.size(); i++) {
                    if (i > 0) expenseQuery.append(" AND ");
                    expenseQuery.append(expenseConditions.get(i));
                }
            }
            
            unionParts.add(expenseQuery.toString());
        }
        
        // Partie Incomes (seulement si type = null ou type = 'income')
        if (type == null || "income".equalsIgnoreCase(type)) {
            StringBuilder incomeQuery = new StringBuilder();
            incomeQuery.append("SELECT ");
            incomeQuery.append("i.id, 'income' as type, ");
            incomeQuery.append("i.amount, i.payment_method as method, i.source, NULL as location, ");
            incomeQuery.append("i.description, i.creation_date as date, ");
            incomeQuery.append("NULL as category_id, ");
            incomeQuery.append("NULL as category_name, ");
            incomeQuery.append("NULL as category_icon, ");
            incomeQuery.append("NULL as category_color ");
            incomeQuery.append("FROM incomes i ");
            incomeQuery.append("WHERE i.user_id = :userId ");
            
            List<String> incomeConditions = new ArrayList<>();
            if (minAmount != null) {
                incomeConditions.add("i.amount >= :minAmount");
            }
            if (maxAmount != null) {
                incomeConditions.add("i.amount <= :maxAmount");
            }
            if (startDateTime != null) {
                incomeConditions.add("i.creation_date >= :startDate");
            }
            if (endDateTime != null) {
                incomeConditions.add("i.creation_date <= :endDate");
            }
            
            if (!incomeConditions.isEmpty()) {
                incomeQuery.append("AND ");
                for (int i = 0; i < incomeConditions.size(); i++) {
                    if (i > 0) incomeQuery.append(" AND ");
                    incomeQuery.append(incomeConditions.get(i));
                }
            }
            
            unionParts.add(incomeQuery.toString());
        }
        
        // Joindre les parties avec UNION ALL
        for (int i = 0; i < unionParts.size(); i++) {
            if (i > 0) sql.append(" UNION ALL ");
            sql.append(unionParts.get(i));
        }
        sql.append(") AS transactions ");
        sql.append("ORDER BY id DESC ");
        sql.append("LIMIT :limit");
        
        // Exécuter la requête avec EntityManager
        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("userId", userId);
        
        if (minAmount != null) {
            query.setParameter("minAmount", minAmount);
        }
        if (maxAmount != null) {
            query.setParameter("maxAmount", maxAmount);
        }
        if (startDateTime != null) {
            query.setParameter("startDate", startDateTime);
        }
        if (endDateTime != null) {
            query.setParameter("endDate", endDateTime);
        }
        query.setParameter("limit", limit);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(mapper::toTransactionDtoFromRow)
                .collect(Collectors.toList());
    }

}

