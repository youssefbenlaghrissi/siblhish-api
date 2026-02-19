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
    private static final double MIN_BUDGET_AMOUNT = 100.0; // Minimum 100 MAD par catégorie (par défaut)
    private static final double MAX_BUDGET_PERCENTAGE_PER_CATEGORY = 0.50; // 50% max par catégorie
    
    /**
     * Contraintes min/max spécifiques par catégorie (basées sur les coûts réels au Maroc)
     * Format: Map<CategoryName, {min, max}>
     * OPTIMISÉ : Cache statique pour éviter la création répétée de la Map
     */
    private static final Map<String, CategoryConstraints> CATEGORY_CONSTRAINTS = createCategoryConstraints();
    
    private static Map<String, CategoryConstraints> createCategoryConstraints() {
        Map<String, CategoryConstraints> constraints = new HashMap<>(4);
        
        // Eau : 50-200 MAD
        constraints.put("Eau", new CategoryConstraints(50.0, 200.0));
        
        // Électricité : 80-300 MAD
        constraints.put("Électricité", new CategoryConstraints(80.0, 300.0));
        
        // Téléphone : 50-300 MAD
        constraints.put("Téléphone", new CategoryConstraints(50.0, 300.0));
        
        // Abonnements : 50-300 MAD
        constraints.put("Abonnements", new CategoryConstraints(50.0, 300.0));
        
        return Collections.unmodifiableMap(constraints);
    }
    
    /**
     * Classe interne pour stocker les contraintes min/max d'une catégorie
     */
    private static class CategoryConstraints {
        final double min;
        final double max;
        
        CategoryConstraints(double min, double max) {
            this.min = min;
            this.max = max;
        }
    }
    
    // Cache statique pour les multiplicateurs de situation
    private static final Map<String, Double> SITUATION_MULTIPLIERS = createSituationMultipliers();
    
    // Cache statique pour les multiplicateurs de localisation
    private static final Map<String, Double> LOCATION_MULTIPLIERS = createLocationMultipliers();
    
    // Cache statique pour les pourcentages par intervalle de revenu
    private static final Map<String, Double> PERCENTAGES_VERY_LOW = createPercentagesForVeryLowIncome();
    private static final Map<String, Double> PERCENTAGES_LOW = createPercentagesForLowIncome();
    private static final Map<String, Double> PERCENTAGES_MEDIUM = initializeCategoryPercentages();
    private static final Map<String, Double> PERCENTAGES_HIGH = createPercentagesForHighIncome();
    private static final Map<String, Double> PERCENTAGES_VERY_HIGH = createPercentagesForVeryHighIncome();
    
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
        
        // OPTIMISATION 2.5 : Obtenir les pourcentages selon l'intervalle de revenu
        Map<String, Double> categoryPercentages = getCategoryPercentages(monthlyIncome);
        
        // OPTIMISATION 3 : Filtrer et calculer en une seule passe
        int size = allSelectedCategories.size();
        List<Category> validCategories = new ArrayList<>(size);
        double totalPercentage = 0.0;
        
        for (Category category : allSelectedCategories) {
            if (!category.getDeleted()) {
                validCategories.add(category);
                String categoryName = category.getName();
                totalPercentage += categoryPercentages.getOrDefault(categoryName, 0.10);
            }
        }
        
        // OPTIMISATION 4 : Calculer le facteur de normalisation une seule fois
        double normalizationFactor = totalPercentage > 1.0 ? 1.0 / totalPercentage : 1.0;
        double maxTotalBudget = monthlyIncome * MAX_BUDGET_PERCENTAGE;
        double maxCategoryBudget = monthlyIncome * MAX_BUDGET_PERCENTAGE_PER_CATEGORY;
        
        // OPTIMISATION 5 : Utiliser le cache statique des contraintes (pas de création de Map)
        // categoryConstraints est déjà en cache statique (CATEGORY_CONSTRAINTS)
        
        // OPTIMISATION 6 : Calculer tous les budgets en une seule passe
        int validSize = validCategories.size();
        List<BudgetSuggestion> suggestions = new ArrayList<>(validSize);
        double totalBudget = 0.0;
        
        for (Category category : validCategories) {
            String categoryName = category.getName();
            
            // Calculer le budget avec les pourcentages adaptés au revenu
            double basePercentage = categoryPercentages.getOrDefault(categoryName, 0.10);
            double budget = monthlyIncome * basePercentage * situationMultiplier * locationMultiplier;
            budget *= normalizationFactor;
            
            // Appliquer les contraintes min/max spécifiques par catégorie
            CategoryConstraints constraints = CATEGORY_CONSTRAINTS.get(categoryName);
            
            if (constraints != null) {
                // Contraintes spécifiques pour cette catégorie
                budget = Math.max(budget, constraints.min);
                budget = Math.min(budget, Math.min(constraints.max, maxCategoryBudget));
            } else {
                // Contraintes par défaut
                budget = Math.max(budget, MIN_BUDGET_AMOUNT);
                budget = Math.min(budget, maxCategoryBudget);
            }
            
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
        
        // OPTIMISATION 7 : Normalisation finale si nécessaire (une seule passe)
        if (totalBudget > maxTotalBudget) {
            double scaleFactor = maxTotalBudget / totalBudget;
            totalBudget = 0.0; // Recalculer le total
            
            for (BudgetSuggestion suggestion : suggestions) {
                String categoryName = suggestion.getCategoryName();
                double scaledAmount = Math.round(suggestion.getAmount() * scaleFactor * 100.0) / 100.0;
                
                // Réappliquer les contraintes spécifiques après normalisation
                CategoryConstraints constraints = CATEGORY_CONSTRAINTS.get(categoryName);
                if (constraints != null) {
                    scaledAmount = Math.max(scaledAmount, constraints.min);
                    scaledAmount = Math.min(scaledAmount, constraints.max);
                }
                
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
     * Obtenir les pourcentages par catégorie selon l'intervalle de revenu
     * Les pourcentages sont plus élevés pour les petits revenus (coûts fixes)
     * et plus faibles pour les grands revenus (économies d'échelle)
     * OPTIMISÉ : Retourne directement les Maps en cache statique (pas de création)
     */
    private Map<String, Double> getCategoryPercentages(double monthlyIncome) {
        // Déterminer l'intervalle de revenu
        IncomeRange range = getIncomeRange(monthlyIncome);
        
        // Retourner les pourcentages depuis le cache statique (O(1) lookup)
        return switch (range) {
            case VERY_LOW -> PERCENTAGES_VERY_LOW;
            case LOW -> PERCENTAGES_LOW;
            case MEDIUM -> PERCENTAGES_MEDIUM;
            case HIGH -> PERCENTAGES_HIGH;
            case VERY_HIGH -> PERCENTAGES_VERY_HIGH;
        };
    }
    
    /**
     * Déterminer l'intervalle de revenu
     */
    private IncomeRange getIncomeRange(double monthlyIncome) {
        if (monthlyIncome < 3000) return IncomeRange.VERY_LOW;      // < 3000 MAD
        if (monthlyIncome < 5000) return IncomeRange.LOW;            // 3000-5000 MAD
        if (monthlyIncome < 10000) return IncomeRange.MEDIUM;        // 5000-10000 MAD
        if (monthlyIncome < 20000) return IncomeRange.HIGH;          // 10000-20000 MAD
        return IncomeRange.VERY_HIGH;                                // >= 20000 MAD
    }
    
    /**
     * Enum pour les intervalles de revenu
     */
    private enum IncomeRange {
        VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH
    }
    
    /**
     * Pourcentages pour revenus très faibles (< 3000 MAD)
     * Pourcentages plus élevés car les coûts fixes représentent une part importante
     * OPTIMISÉ : Méthode statique pour initialisation du cache
     */
    private static Map<String, Double> createPercentagesForVeryLowIncome() {
        Map<String, Double> percentages = new HashMap<>();
        
        // ALIMENTATION - Plus élevé pour petits revenus (coûts fixes)
        percentages.put("Alimentation", 0.30);      // 30% - Augmenté pour refléter la réalité
        percentages.put("Restaurant", 0.02);         // 2% - Réduit
        percentages.put("Café", 0.01);              // 1% - Réduit
        
        // TRANSPORT & VOITURE (même part du revenu que Transport)
        percentages.put("Transport", 0.10);          // 10% - Plus élevé
        percentages.put("Voiture", 0.10);            // 10% - Idem Transport
        percentages.put("Carburant", 0.10);         // 10% - Plus élevé
        percentages.put("Parking", 0.01);
        percentages.put("Vignette", 0.005);
        percentages.put("Assurance voiture", 0.02);
        percentages.put("Entretien voiture", 0.02);
        percentages.put("Contrôle technique", 0.003);
        percentages.put("Péage", 0.005);
        percentages.put("Lavage voiture", 0.005);
        
        // LOGEMENT - Plus élevé (coûts fixes)
        percentages.put("Loyer", 0.35);              // 35% - Plus élevé
        percentages.put("Électricité", 0.05);       // 5% - Plus élevé
        percentages.put("Eau", 0.03);                // 3% - Plus élevé
        percentages.put("Internet", 0.03);          // 3% - Plus élevé
        percentages.put("Gaz", 0.03);               // 3% - Plus élevé
        
        // SANTÉ
        percentages.put("Santé", 0.05);              // 5% - Plus élevé
        percentages.put("Médical", 0.03);           // 3% - Plus élevé
        percentages.put("Pharmacie", 0.03);         // 3% - Très faible
        percentages.put("Salle de sport", 0.01);
        percentages.put("Sport", 0.01);
        
        // FINANCE
        percentages.put("Crédit", 0.05);
        percentages.put("Assurance", 0.03);
        percentages.put("Impôts", 0.02);
        percentages.put("Épargne", 0.01);          // 1% - Réduit
        
        // LOISIRS - Réduit
        percentages.put("Loisirs", 0.02);
        percentages.put("Voyage", 0.01);
        percentages.put("Sorties", 0.01);
        percentages.put("Streaming", 0.01);
        percentages.put("Cinéma", 0.015);            // 1.5% - Très faible
        percentages.put("Livres", 0.01);            // 1% - Très faible
        
        // SHOPPING - Réduit
        percentages.put("Shopping", 0.01);
        percentages.put("Vêtements", 0.01);
        percentages.put("Électronique", 0.01);
        
        // FAMILLE
        percentages.put("Éducation", 0.03);
        percentages.put("Enfants", 0.02);
        percentages.put("Cadeaux", 0.01);
        
        // ABONNEMENTS
        percentages.put("Abonnements", 0.02);
        percentages.put("Téléphone", 0.02);
        
        // AUTRES
        percentages.put("Factures", 0.02);
        percentages.put("Animaux", 0.01);
        percentages.put("Beauté", 0.01);
        percentages.put("Pour femme", 0.08);         // 8% - Très faible
        percentages.put("Pour madame", 0.08);       // 8% - Alias
        percentages.put("Autres", 0.02);
        
        return Collections.unmodifiableMap(percentages);
    }
    
    /**
     * Pourcentages pour revenus faibles (3000-5000 MAD)
     * OPTIMISÉ : Méthode statique pour initialisation du cache
     */
    private static Map<String, Double> createPercentagesForLowIncome() {
        Map<String, Double> percentages = new HashMap<>();
        
        percentages.put("Alimentation", 0.25);      // 25% - Augmenté pour refléter la réalité
        percentages.put("Restaurant", 0.03);
        percentages.put("Café", 0.015);
        
        percentages.put("Transport", 0.075);        // 7.5%
        percentages.put("Voiture", 0.075);          // 7.5% - Idem Transport
        percentages.put("Carburant", 0.08);
        percentages.put("Parking", 0.01);
        percentages.put("Vignette", 0.005);
        percentages.put("Assurance voiture", 0.015);
        percentages.put("Entretien voiture", 0.015);
        percentages.put("Contrôle technique", 0.003);
        percentages.put("Péage", 0.005);
        percentages.put("Lavage voiture", 0.007);
        
        percentages.put("Loyer", 0.30);              // 30%
        percentages.put("Électricité", 0.04);
        percentages.put("Eau", 0.025);
        percentages.put("Internet", 0.025);
        percentages.put("Gaz", 0.025);
        
        percentages.put("Santé", 0.04);
        percentages.put("Médical", 0.025);
        percentages.put("Pharmacie", 0.025);        // 2.5% - Faible
        percentages.put("Salle de sport", 0.012);
        percentages.put("Sport", 0.012);
        
        percentages.put("Crédit", 0.04);
        percentages.put("Assurance", 0.025);
        percentages.put("Impôts", 0.02);
        percentages.put("Épargne", 0.015);
        
        percentages.put("Loisirs", 0.03);
        percentages.put("Voyage", 0.02);
        percentages.put("Sorties", 0.015);
        percentages.put("Streaming", 0.015);
        percentages.put("Cinéma", 0.015);           // 1.5% - Faible
        percentages.put("Livres", 0.01);            // 1% - Faible
        
        percentages.put("Shopping", 0.02);
        percentages.put("Vêtements", 0.02);
        percentages.put("Électronique", 0.02);
        
        percentages.put("Éducation", 0.04);
        percentages.put("Enfants", 0.025);
        percentages.put("Cadeaux", 0.015);
        
        percentages.put("Abonnements", 0.018);
        percentages.put("Téléphone", 0.018);
        
        percentages.put("Factures", 0.02);
        percentages.put("Animaux", 0.015);
        percentages.put("Beauté", 0.015);
        percentages.put("Pour femme", 0.08);         // 8% - Faible
        percentages.put("Pour madame", 0.08);       // 8% - Alias
        percentages.put("Autres", 0.025);
        
        return Collections.unmodifiableMap(percentages);
    }
    
    // Note: PERCENTAGES_MEDIUM est initialisé directement avec initializeCategoryPercentages()
    
    /**
     * Pourcentages pour revenus élevés (10000-20000 MAD)
     * OPTIMISÉ : Méthode statique pour initialisation du cache
     */
    private static Map<String, Double> createPercentagesForHighIncome() {
        Map<String, Double> percentages = new HashMap<>();
        
        percentages.put("Alimentation", 0.15);      // 15% - Augmenté pour refléter la réalité
        percentages.put("Restaurant", 0.05);        // 5% - Augmenté
        percentages.put("Café", 0.025);
        
        percentages.put("Transport", 0.03);         // 3%
        percentages.put("Voiture", 0.03);            // 3% - Idem Transport
        percentages.put("Carburant", 0.035);
        percentages.put("Parking", 0.01);
        percentages.put("Vignette", 0.005);
        percentages.put("Assurance voiture", 0.008);
        percentages.put("Entretien voiture", 0.008);
        percentages.put("Contrôle technique", 0.003);
        percentages.put("Péage", 0.005);
        percentages.put("Lavage voiture", 0.007);
        
        percentages.put("Loyer", 0.18);             // 18% - Réduit
        percentages.put("Électricité", 0.025);
        percentages.put("Eau", 0.015);
        percentages.put("Internet", 0.012);
        percentages.put("Gaz", 0.012);
        
        percentages.put("Santé", 0.025);
        percentages.put("Médical", 0.018);
        percentages.put("Pharmacie", 0.018);        // 1.8% - Élevé
        percentages.put("Salle de sport", 0.018);
        percentages.put("Sport", 0.018);
        
        percentages.put("Crédit", 0.04);
        percentages.put("Assurance", 0.018);
        percentages.put("Impôts", 0.025);
        percentages.put("Épargne", 0.025);          // 2.5% - Augmenté
        
        percentages.put("Loisirs", 0.05);
        percentages.put("Voyage", 0.05);
        percentages.put("Sorties", 0.025);
        percentages.put("Streaming", 0.025);
        percentages.put("Cinéma", 0.012);           // 1.2% - Élevé
        percentages.put("Livres", 0.008);          // 0.8% - Élevé
        
        percentages.put("Shopping", 0.04);
        percentages.put("Vêtements", 0.03);
        percentages.put("Électronique", 0.03);
        
        percentages.put("Éducation", 0.06);
        percentages.put("Enfants", 0.04);
        percentages.put("Cadeaux", 0.025);
        
        percentages.put("Abonnements", 0.015);
        percentages.put("Téléphone", 0.015);
        
        percentages.put("Factures", 0.02);
        percentages.put("Animaux", 0.025);
        percentages.put("Beauté", 0.025);
        percentages.put("Pour femme", 0.04);         // 4% - Élevé
        percentages.put("Pour madame", 0.04);       // 4% - Alias
        percentages.put("Autres", 0.04);
        
        return Collections.unmodifiableMap(percentages);
    }
    
    /**
     * Pourcentages pour revenus très élevés (>= 20000 MAD)
     * OPTIMISÉ : Méthode statique pour initialisation du cache
     */
    private static Map<String, Double> createPercentagesForVeryHighIncome() {
        Map<String, Double> percentages = new HashMap<>();
        
        percentages.put("Alimentation", 0.12);       // 12% - Augmenté pour refléter la réalité
        percentages.put("Restaurant", 0.06);         // 6% - Augmenté
        percentages.put("Café", 0.03);
        
        percentages.put("Transport", 0.03);        // 3%
        percentages.put("Voiture", 0.03);            // 3% - Idem Transport
        percentages.put("Carburant", 0.03);
        percentages.put("Parking", 0.01);
        percentages.put("Vignette", 0.005);
        percentages.put("Assurance voiture", 0.008);
        percentages.put("Entretien voiture", 0.008);
        percentages.put("Contrôle technique", 0.003);
        percentages.put("Péage", 0.005);
        percentages.put("Lavage voiture", 0.007);
        
        percentages.put("Loyer", 0.15);              // 15% - Encore plus réduit
        percentages.put("Électricité", 0.02);
        percentages.put("Eau", 0.012);
        percentages.put("Internet", 0.01);
        percentages.put("Gaz", 0.01);
        
        percentages.put("Santé", 0.02);
        percentages.put("Médical", 0.015);
        percentages.put("Pharmacie", 0.012);        // 1.2% - Très élevé
        percentages.put("Salle de sport", 0.02);
        percentages.put("Sport", 0.02);
        
        percentages.put("Crédit", 0.04);
        percentages.put("Assurance", 0.015);
        percentages.put("Impôts", 0.03);
        percentages.put("Épargne", 0.05);           // 5% - Beaucoup augmenté
        
        percentages.put("Loisirs", 0.06);
        percentages.put("Voyage", 0.08);            // 8% - Augmenté
        percentages.put("Sorties", 0.03);
        percentages.put("Streaming", 0.03);
        percentages.put("Cinéma", 0.01);            // 1% - Très élevé
        percentages.put("Livres", 0.006);           // 0.6% - Très élevé
        
        percentages.put("Shopping", 0.05);
        percentages.put("Vêtements", 0.04);
        percentages.put("Électronique", 0.04);
        
        percentages.put("Éducation", 0.08);
        percentages.put("Enfants", 0.06);
        percentages.put("Cadeaux", 0.03);
        
        percentages.put("Abonnements", 0.015);
        percentages.put("Téléphone", 0.015);
        
        percentages.put("Factures", 0.02);
        percentages.put("Animaux", 0.03);
        percentages.put("Beauté", 0.03);
        percentages.put("Pour femme", 0.03);         // 3% - Très élevé
        percentages.put("Pour madame", 0.03);       // 3% - Alias
        percentages.put("Autres", 0.05);
        
        return Collections.unmodifiableMap(percentages);
    }
    
    /**
     * Initialiser les pourcentages par catégorie (méthode statique appelée une seule fois)
     * Table de référence : pourcentage standard du revenu alloué à chaque catégorie
     * Basé sur la règle 50/30/20 (Essentiels/Personnel/Épargne)
     * Utilisé pour les revenus moyens (5000-10000 MAD)
     */
    private static Map<String, Double> initializeCategoryPercentages() {
        Map<String, Double> percentages = new HashMap<>();
        
        // ========== ALIMENTATION (18% du revenu) ==========
        percentages.put("Alimentation", 0.18);      // 18% - Augmenté pour refléter la réalité
        percentages.put("Restaurant", 0.04);         // 4% - Restaurants
        percentages.put("Café", 0.02);              // 2% - Cafés
        
        // ========== TRANSPORT & VOITURE (12% du revenu) ==========
        // Transport % par tranche: Très faible 10%, Faible 7.5%, Moyen 4%, Élevé 3%, Très élevé 3%
        percentages.put("Transport", 0.04);          // 4% - Transport général (revenus moyens 5000-10000 MAD)
        percentages.put("Voiture", 0.04);           // 4% - Idem Transport
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
        percentages.put("Pharmacie", 0.02);          // 2% - Moyen
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
        percentages.put("Cinéma", 0.012);           // 1.2% - Moyen
        percentages.put("Livres", 0.008);            // 0.8% - Moyen
        
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
        percentages.put("Pour femme", 0.05);         // 5% - Moyen
        percentages.put("Pour madame", 0.05);       // 5% - Alias
        percentages.put("Autres", 0.03);             // 3% - Autres dépenses
        
        return Collections.unmodifiableMap(percentages);
    }
}

