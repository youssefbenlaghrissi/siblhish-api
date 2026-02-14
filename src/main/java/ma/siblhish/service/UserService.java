package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Favorite;
import ma.siblhish.entities.User;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.FavoriteRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EntityMapper mapper;
    private final FavoriteRepository favoriteRepository;

    public UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return mapper.toUserProfileDto(user);
    }

    /**
     * Authentification sociale - trouve ou crée un utilisateur et retourne son profil
     * 
     * @param email Email de l'utilisateur
     * @param displayName Nom d'affichage
     * @param provider Provider (google, facebook, etc.)
     * @param notificationsEnabled Statut des notifications (true/false/null)
     *                             - Si utilisateur existant : garde son statut existant
     *                             - Si nouvel utilisateur : utilise la valeur envoyée
     */
    @Transactional
    public UserProfileDto socialLogin(String email, String displayName, String provider, Boolean notificationsEnabled) {
        User user = findOrCreateByEmail(email, displayName, provider, notificationsEnabled);
        return mapper.toUserProfileDto(user);
    }

    /**
     * Trouve un utilisateur par email ou en crée un nouveau
     * 
     * @param email Email de l'utilisateur
     * @param displayName Nom d'affichage
     * @param provider Provider (google, facebook, etc.)
     * @param notificationsEnabled Statut des notifications (true/false/null)
     *                             - Si utilisateur existant : garde son statut existant (ignoré)
     *                             - Si nouvel utilisateur : utilise la valeur envoyée
     */
    @Transactional
    protected User findOrCreateByEmail(String email, String displayName, String provider, Boolean notificationsEnabled) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // Nouvel utilisateur : créer avec notificationsEnabled envoyé
                    String[] names = displayName != null ? displayName.split(" ", 2) : new String[]{"User"};
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstName(names.length > 0 ? names[0] : "User");
                    newUser.setLastName(names.length > 1 ? names[1] : "");
                    newUser.setPassword("oauth_" + provider);
                    newUser.setLanguage("fr");
                    // Utiliser notificationsEnabled envoyé, ou true par défaut si null
                    newUser.setNotificationsEnabled(notificationsEnabled != null ? notificationsEnabled : true);
                    LocalDateTime now = LocalDateTime.now();
                    newUser.setCreationDate(now);
                    User savedUser = userRepository.save(newUser);
                    
                    // Créer les favoris par défaut
                    initializeDefaultFavorites(savedUser);
                    
                    return savedUser;
                });
        // Si utilisateur existant : son notificationsEnabled existant est conservé (pas de modification)
    }

    /**
     * Initialiser les favoris par défaut pour un nouvel utilisateur
     * Assigne bar_chart (id=1) et pie_chart (id=2) par défaut
     */
    private void initializeDefaultFavorites(User user) {
        List<Favorite> defaultFavorites = new ArrayList<>();
        
        // Carte bar_chart (id=1) - Graphique Revenus vs Dépenses
        Favorite barChartFavorite = new Favorite();
        barChartFavorite.setUser(user);
        barChartFavorite.setType("CARD");
        barChartFavorite.setTargetEntity(1L); // ID de la carte bar_chart
        barChartFavorite.setValue("1");
        defaultFavorites.add(barChartFavorite);
        
        // Carte pie_chart (id=2) - Répartition par Catégorie
        Favorite pieChartFavorite = new Favorite();
        pieChartFavorite.setUser(user);
        pieChartFavorite.setType("CARD");
        pieChartFavorite.setTargetEntity(2L); // ID de la carte pie_chart
        pieChartFavorite.setValue("2");
        defaultFavorites.add(pieChartFavorite);
        
        favoriteRepository.saveAll(defaultFavorites);
    }

    /**
     * Mettre à jour le token FCM d'un utilisateur
     * 
     * @param userId ID de l'utilisateur
     * @param fcmToken Token FCM à enregistrer
     * @throws RuntimeException si l'utilisateur n'existe pas
     */
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    /**
     * Mettre à jour uniquement les préférences utilisateur
     * (notificationsEnabled et language)
     * 
     * @param userId ID de l'utilisateur
     * @param notificationsEnabled Nouveau statut des notifications (peut être null)
     * @param language Nouvelle langue (peut être null)
     * @return UserProfileDto mis à jour
     * @throws RuntimeException si l'utilisateur n'existe pas
     */
    @Transactional
    public UserProfileDto updatePreferences(Long userId, Boolean notificationsEnabled, String language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // Mettre à jour uniquement si les valeurs sont fournies
        if (notificationsEnabled != null) {
            user.setNotificationsEnabled(notificationsEnabled);
        }
        
        if (language != null && !language.trim().isEmpty()) {
            user.setLanguage(language);
        }
        
        User savedUser = userRepository.save(user);
        return mapper.toUserProfileDto(savedUser);
    }
}

