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
     */
    @Transactional
    public UserProfileDto socialLogin(String email, String displayName, String provider) {
        User user = findOrCreateByEmail(email, displayName, provider);
        return mapper.toUserProfileDto(user);
    }

    /**
     * Trouve un utilisateur par email ou en crée un nouveau
     */
    @Transactional
    private User findOrCreateByEmail(String email, String displayName, String provider) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    String[] names = displayName != null ? displayName.split(" ", 2) : new String[]{"User"};
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstName(names.length > 0 ? names[0] : "User");
                    newUser.setLastName(names.length > 1 ? names[1] : "");
                    newUser.setPassword("oauth_" + provider);
                    newUser.setLanguage("fr");
                    LocalDateTime now = LocalDateTime.now();
                    newUser.setCreationDate(now);
                    newUser.setUpdateDate(now);
                    User savedUser = userRepository.save(newUser);
                    
                    // Créer les favoris par défaut
                    initializeDefaultFavorites(savedUser);
                    
                    return savedUser;
                });
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
}

