package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.siblhish.dto.*;
import ma.siblhish.entities.Favorite;
import ma.siblhish.entities.User;
import ma.siblhish.exception.AuthenticationException;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.FavoriteRepository;
import ma.siblhish.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EntityMapper mapper;
    private final FavoriteRepository favoriteRepository;
    private final PasswordEncoder passwordEncoder;

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
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            // Vérifier si le compte est supprimé
            if (Boolean.TRUE.equals(existingUser.getDeleted())) {
                throw new IllegalArgumentException("Ce compte a été supprimé. Veuillez contacter le support pour le réactiver.");
            }
            // Si utilisateur existant et non supprimé : retourner l'utilisateur existant
            return existingUser;
        }
        
        // Nouvel utilisateur : créer avec notificationsEnabled envoyé
        // IMPORTANT: l'entité User a des contraintes @NotBlank sur firstName/lastName,
        // donc on garantit toujours des valeurs non vides même si displayName est incomplet.
        String safeDisplayName = displayName != null ? displayName.trim() : "";
        String[] names = !safeDisplayName.isEmpty() ? safeDisplayName.split("\\s+", 2) : new String[]{"User"};
        User newUser = new User();
        newUser.setEmail(email);
        String firstName = (names.length > 0 && names[0] != null && !names[0].trim().isEmpty()) ? names[0].trim() : "User";
        String lastName = (names.length > 1 && names[1] != null && !names[1].trim().isEmpty()) ? names[1].trim() : firstName;
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
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
     * @throws IllegalArgumentException si le token est vide ou si l'utilisateur n'existe pas
     */
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Le token FCM est requis");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

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
     * @throws IllegalArgumentException si l'utilisateur n'existe pas ou si la langue est invalide
     */
    @Transactional
    public UserProfileDto updatePreferences(Long userId, Boolean notificationsEnabled, String language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        // Mettre à jour uniquement si les valeurs sont fournies
        if (notificationsEnabled != null) {
            user.setNotificationsEnabled(notificationsEnabled);
        }
        
        if (language != null && !language.trim().isEmpty()) {
            // Valider que la langue est supportée (fr, en, ar)
            String lang = language.trim().toLowerCase();
            if (!lang.equals("fr") && !lang.equals("en") && !lang.equals("ar")) {
                throw new IllegalArgumentException("Langue non supportée. Les langues supportées sont: fr, en, ar");
            }
            user.setLanguage(lang);
        }
        
        User savedUser = userRepository.save(user);
        return mapper.toUserProfileDto(savedUser);
    }

    /**
     * Créer un nouveau compte utilisateur avec email et mot de passe
     * 
     * @param firstName Prénom
     * @param lastName Nom
     * @param email Email
     * @param password Mot de passe (sera hashé)
     * @param language Langue (défaut: "fr")
     * @param notificationsEnabled Statut des notifications
     * @return UserProfileDto
     * @throws IllegalArgumentException si l'email existe déjà
     */
    @Transactional
    public UserProfileDto register(
            String firstName,
            String lastName,
            String email,
            String password,
            String language,
            Boolean notificationsEnabled) {
        
        // Vérifier si l'email existe déjà
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            // Vérifier si le compte est supprimé
            if (Boolean.TRUE.equals(existingUser.getDeleted())) {
                throw new IllegalArgumentException("Ce compte a été supprimé. Veuillez contacter le support pour le réactiver.");
            }
            // Si le compte existe et n'est pas supprimé
            throw new IllegalArgumentException("Un compte avec cet email existe déjà");
        }

        // Créer un nouvel utilisateur
        User newUser = new User();
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        
        // Hasher le mot de passe avec BCrypt
        newUser.setPassword(passwordEncoder.encode(password));
        
        // Définir la langue (par défaut "fr" si non fourni)
        newUser.setLanguage(language != null ? language : "fr");
        
        // Définir le statut des notifications (par défaut true si non fourni)
        newUser.setNotificationsEnabled(
            notificationsEnabled != null 
                ? notificationsEnabled 
                : true
        );

        LocalDateTime now = LocalDateTime.now();
        newUser.setCreationDate(now);

        // Sauvegarder l'utilisateur
        User savedUser = userRepository.save(newUser);
        
        log.info("✅ Utilisateur créé avec succès - ID: {}, Email: {}", 
            savedUser.getId(), savedUser.getEmail());

        // Créer les favoris par défaut
        initializeDefaultFavorites(savedUser);

        // Convertir en DTO
        return mapper.toUserProfileDto(savedUser);
    }

    /**
     * Authentifier un utilisateur avec email et mot de passe
     *
     * @param email Email
     * @param password Mot de passe (non hashé)
     * @return UserProfileDto
     * @throws AuthenticationException si l'email ou le mot de passe est incorrect,
     *                                 si le compte est supprimé ou si c'est un compte social
     */
    public UserProfileDto login(String email, String password) {
        // Trouver l'utilisateur par email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("Email ou mot de passe incorrect"));

        // Vérifier si le compte est supprimé
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new AuthenticationException("Ce compte a été supprimé. Veuillez contacter le support pour le réactiver.");
        }

        // Vérifier si c'est un utilisateur OAuth (password commence par "oauth_")
        if (user.getPassword() != null && user.getPassword().startsWith("oauth_")) {
            throw new AuthenticationException("Ce compte a été créé avec une connexion sociale. Veuillez utiliser la connexion Google.");
        }

        // Vérifier le mot de passe avec BCrypt
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationException("Email ou mot de passe incorrect");
        }

        log.info("✅ Utilisateur authentifié avec succès - ID: {}, Email: {}",
                user.getId(), user.getEmail());

        // Convertir en DTO
        return mapper.toUserProfileDto(user);
    }

    /**
     * Supprimer le compte utilisateur (soft delete)
     * Met le champ deleted à true
     * 
     * @param userId ID de l'utilisateur
     * @throws IllegalArgumentException si l'utilisateur n'existe pas ou si le compte est déjà supprimé
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        // Vérifier si le compte n'est pas déjà supprimé
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new IllegalArgumentException("Le compte a déjà été supprimé");
        }
        
        // Soft delete : mettre deleted à true
        user.setDeleted(true);
        userRepository.save(user);
        
        log.info("✅ Compte utilisateur supprimé (soft delete) - ID: {}, Email: {}", 
            user.getId(), user.getEmail());
    }
}

