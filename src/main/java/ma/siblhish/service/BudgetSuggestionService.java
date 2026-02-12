package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.BudgetSuggestion;
import ma.siblhish.dto.BudgetSuggestionRequest;
import ma.siblhish.dto.BudgetSuggestionResponse;
import ma.siblhish.entities.Category;
import ma.siblhish.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service pour calculer les suggestions de budgets
 */
@Service
@RequiredArgsConstructor
public class BudgetSuggestionService {
    
    private final CategoryRepository categoryRepository;
    
    // Constantes
    private static final double MAX_BUDGET_PERCENTAGE = 0.80; // 80% du revenu maximum
    private static final double MIN_BUDGET_AMOUNT = 100.0; // Minimum 100 MAD par catégorie
    private static final double MAX_BUDGET_PERCENTAGE_PER_CATEGORY = 0.50; // 50% max par catégorie
    
    // Cache statique pour les pourcentages de catégories (ne change jamais)
    private static final Map<String, Double> CATEGORY_PERCENTAGES = initializeCategoryPercentages();
    
    // Cache statique pour les multiplicateurs de situation
    private static final Map<String, Double> SITUATION_MULTIPLIERS = createSituationMultipliers();
    
    // Cache statique pour les multiplicateurs de localisation
    private static final Map<String, Double> LOCATION_MULTIPLIERS = createLocationMultipliers();
    
    private static Map<String, Double> createSituationMultipliers() {
        Map<String, Double> map = new HashMap<>(4);
        map.put("Célibataire", 1.0);
        map.put("En couple", 1.5);
        map.put("Famille", 2.2);
        map.put("Étudiant", 0.7);
        return Collections.unmodifiableMap(map);
    }
    
    private static Map<String, Double> createLocationMultipliers() {
        Map<String, Double> map = new HashMap<>(2);
        map.put("ville", 1.15);
        map.put("campagne", 0.85);
        return Collections.unmodifiableMap(map);
    }
    
    // Comparateur pour tri décroissant (créé une seule fois)
    private static final Comparator<BudgetSuggestion> AMOUNT_DESC_COMPARATOR = 
        (a, b) -> Double.compare(b.getAmount(), a.getAmount());
    
    /**
     * Méthode principale pour suggérer des budgets
     */
    public BudgetSuggestionResponse suggestBudgets(BudgetSuggestionRequest request) {
        double monthlyIncome = request.getMonthlyIncome();
        String situation = request.getSituation();
        String location = request.getLocation();
        List<Long> categoryIds = request.getCategoryIds();
        
        // Calculer les budgets avec le total déjà calculé
        BudgetCalculationResult result = calculateBudgets(
            monthlyIncome,
            situation,
            location,
            categoryIds
        );
        
        // Trier par montant décroissant (comparateur réutilisé)
        result.suggestions.sort(AMOUNT_DESC_COMPARATOR);
        
        // Calculer l'épargne suggérée
        double suggestedSavings = monthlyIncome - result.totalBudget;
        
        // Construire la réponse
        return BudgetSuggestionResponse.builder()
            .monthlyIncome(monthlyIncome)
            .situation(situation)
            .location(location)
            .totalSuggestedBudget(result.totalBudget)
            .suggestedSavings(Math.round(suggestedSavings * 100.0) / 100.0)
            .budgets(result.suggestions)
            .build();
    }
    
    /**
     * Classe interne pour retourner le résultat du calcul
     */
    private static class BudgetCalculationResult {
        final List<BudgetSuggestion> suggestions;
        final double totalBudget;
        
        BudgetCalculationResult(List<BudgetSuggestion> suggestions, double totalBudget) {
            this.suggestions = suggestions;
            this.totalBudget = totalBudget;
        }
    }
    
    /**
     * Calculer les budgets pour chaque catégorie sélectionnée
     */
    private BudgetCalculationResult calculateBudgets(
        double monthlyIncome,
        String situation,
        String location,
        List<Long> categoryIds
    ) {
        // OPTIMISATION 1 : Récupérer les multiplicateurs depuis le cache (O(1) au lieu de switch)
        double situationMultiplier = SITUATION_MULTIPLIERS.getOrDefault(situation, 1.0);
        double locationMultiplier = LOCATION_MULTIPLIERS.getOrDefault(location.toLowerCase(), 1.0);
        
        // OPTIMISATION 2 : Récupérer toutes les catégories en une seule requête
        List<Category> allSelectedCategories = categoryRepository.findAllById(categoryIds);
        
        // OPTIMISATION 3 : Filtrer et calculer en une seule passe
        int size = allSelectedCategories.size();
        List<Category> validCategories = new ArrayList<>(size);
        double totalPercentage = 0.0;
        
        for (Category category : allSelectedCategories) {
            if (!category.getDeleted()) {
                validCategories.add(category);
                String categoryName = category.getName();
                totalPercentage += CATEGORY_PERCENTAGES.getOrDefault(categoryName, 0.10);
            }
        }
        
        // OPTIMISATION 4 : Calculer le facteur de normalisation une seule fois
        double normalizationFactor = totalPercentage > 1.0 ? 1.0 / totalPercentage : 1.0;
        double maxTotalBudget = monthlyIncome * MAX_BUDGET_PERCENTAGE;
        double maxCategoryBudget = monthlyIncome * MAX_BUDGET_PERCENTAGE_PER_CATEGORY;
        
        // OPTIMISATION 5 : Calculer tous les budgets en une seule passe
        int validSize = validCategories.size();
        List<BudgetSuggestion> suggestions = new ArrayList<>(validSize);
        double totalBudget = 0.0;
        
        for (Category category : validCategories) {
            String categoryName = category.getName();
            
            // Calculer le budget
            double basePercentage = CATEGORY_PERCENTAGES.getOrDefault(categoryName, 0.10);
            double budget = monthlyIncome * basePercentage * situationMultiplier * locationMultiplier;
            budget *= normalizationFactor;
            
            // Appliquer les contraintes min/max
            budget = Math.max(budget, MIN_BUDGET_AMOUNT);
            budget = Math.min(budget, maxCategoryBudget);
            
            // Arrondir le montant (une seule fois)
            budget = Math.round(budget * 100.0) / 100.0;
            
            // Calculer le pourcentage final : (budget / monthlyIncome) * 100
            // Exemple: 2070 / 15000 = 0.138, puis 0.138 * 100 = 13.8%
            double finalPercentage = Math.round((budget / monthlyIncome) * 10000.0) / 100.0;
            
            suggestions.add(new BudgetSuggestion(
                category.getId(),
                categoryName,
                budget,
                finalPercentage,
                category.getIcon(),
                category.getColor()
            ));
            
            totalBudget += budget;
        }
        
        // OPTIMISATION 6 : Normalisation finale si nécessaire (une seule passe)
        if (totalBudget > maxTotalBudget) {
            double scaleFactor = maxTotalBudget / totalBudget;
            totalBudget = 0.0; // Recalculer le total
            
            for (BudgetSuggestion suggestion : suggestions) {
                double scaledAmount = Math.round(suggestion.getAmount() * scaleFactor * 100.0) / 100.0;
                suggestion.setAmount(scaledAmount);
                // Calculer le pourcentage : (scaledAmount / monthlyIncome) * 100
                suggestion.setPercentage(Math.round((scaledAmount / monthlyIncome) * 10000.0) / 100.0);
                totalBudget += scaledAmount;
            }
        } else {
            // Arrondir le total une seule fois
            totalBudget = Math.round(totalBudget * 100.0) / 100.0;
        }
        
        return new BudgetCalculationResult(suggestions, totalBudget);
    }
    
    /**
     * Initialiser les pourcentages par catégorie (méthode statique appelée une seule fois)
     * Table de référence : pourcentage standard du revenu alloué à chaque catégorie
     * Basé sur la règle 50/30/20 (Essentiels/Personnel/Épargne)
     */
    private static Map<String, Double> initializeCategoryPercentages() {
        Map<String, Double> percentages = new HashMap<>();
        
        // ========== ALIMENTATION (18% du revenu) ==========
        percentages.put("Alimentation", 0.12);      // 12% - Base alimentation
        percentages.put("Restaurant", 0.04);         // 4% - Restaurants
        percentages.put("Café", 0.02);              // 2% - Cafés
        
        // ========== TRANSPORT & VOITURE (12% du revenu) ==========
        percentages.put("Transport", 0.03);          // 3% - Transport général
        percentages.put("Carburant", 0.04);         // 4% - Essence/Diesel
        percentages.put("Parking", 0.01);           // 1% - Parking
        percentages.put("Vignette", 0.005);          // 0.5% - Vignette (annuel divisé par 12)
        percentages.put("Assurance voiture", 0.01); // 1% - Assurance auto
        percentages.put("Entretien voiture", 0.01); // 1% - Réparations/Entretien
        percentages.put("Contrôle technique", 0.003); // 0.3% - Contrôle (annuel/12)
        percentages.put("Péage", 0.005);            // 0.5% - Péages
        percentages.put("Lavage voiture", 0.007);   // 0.7% - Lavage
        
        // ========== LOGEMENT (28% du revenu) ==========
        percentages.put("Loyer", 0.20);              // 20% - Loyer principal
        percentages.put("Électricité", 0.03);       // 3% - Électricité
        percentages.put("Eau", 0.02);                // 2% - Eau
        percentages.put("Internet", 0.015);          // 1.5% - Internet
        percentages.put("Gaz", 0.015);               // 1.5% - Gaz
        // Note: Si "Logement" existe comme catégorie parente, elle sera ajoutée automatiquement
        
        // ========== SANTÉ & SPORT (8% du revenu) ==========
        percentages.put("Santé", 0.03);              // 3% - Santé générale
        percentages.put("Médical", 0.02);            // 2% - Soins médicaux
        percentages.put("Salle de sport", 0.015);   // 1.5% - Abonnement salle
        percentages.put("Sport", 0.015);             // 1.5% - Équipement sport
        
        // ========== FINANCE (10% du revenu) ==========
        percentages.put("Crédit", 0.04);             // 4% - Remboursements crédit
        percentages.put("Assurance", 0.02);           // 2% - Assurances diverses
        percentages.put("Impôts", 0.02);             // 2% - Impôts
        percentages.put("Épargne", 0.02);            // 2% - Épargne (déjà comptée séparément)
        
        // ========== LOISIRS (12% du revenu) ==========
        percentages.put("Loisirs", 0.04);            // 4% - Loisirs généraux
        percentages.put("Voyage", 0.04);             // 4% - Voyages
        percentages.put("Sorties", 0.02);            // 2% - Sorties
        percentages.put("Streaming", 0.02);           // 2% - Abonnements streaming
        
        // ========== SHOPPING (8% du revenu) ==========
        percentages.put("Shopping", 0.03);            // 3% - Shopping général
        percentages.put("Vêtements", 0.025);         // 2.5% - Vêtements
        percentages.put("Électronique", 0.025);     // 2.5% - Électronique
        
        // ========== FAMILLE (10% du revenu) ==========
        percentages.put("Éducation", 0.05);          // 5% - Éducation
        percentages.put("Enfants", 0.03);             // 3% - Dépenses enfants
        percentages.put("Cadeaux", 0.02);             // 2% - Cadeaux
        
        // ========== ABONNEMENTS (3% du revenu) ==========
        percentages.put("Abonnements", 0.015);       // 1.5% - Abonnements divers
        percentages.put("Téléphone", 0.015);         // 1.5% - Téléphone mobile
        
        // ========== AUTRES (9% du revenu) ==========
        percentages.put("Factures", 0.02);           // 2% - Factures diverses
        percentages.put("Animaux", 0.02);            // 2% - Animaux de compagnie
        percentages.put("Beauté", 0.02);             // 2% - Soins beauté
        percentages.put("Autres", 0.03);             // 3% - Autres dépenses
        
        return percentages;
    }
}

