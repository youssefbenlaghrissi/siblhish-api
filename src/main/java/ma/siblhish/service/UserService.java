package ma.siblhish.service;

import lombok.RequiredArgsConstructor;
import ma.siblhish.dto.*;
import ma.siblhish.entities.User;
import ma.siblhish.enums.UserType;
import ma.siblhish.mapper.EntityMapper;
import ma.siblhish.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EntityMapper mapper;

    public UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return mapper.toUserProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UserProfileUpdateDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (request.getType() != null) user.setType(request.getType());
        if (request.getLanguage() != null) user.setLanguage(request.getLanguage());
        if (request.getMonthlySalary() != null) user.setMonthlySalary(request.getMonthlySalary());
        if (request.getNotificationsEnabled() != null) user.setNotificationsEnabled(request.getNotificationsEnabled());
        
        User saved = userRepository.save(user);
        return mapper.toUserProfileDto(saved);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        // TODO: Implement password hashing and validation
        if (!user.getPassword().equals(request.getCurrentPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        user.setPassword(request.getNewPassword());
        userRepository.save(user);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public User findOrCreateByEmail(String email, String displayName, String provider) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    String[] names = displayName.split(" ", 2);
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFirstName(names.length > 0 ? names[0] : "User");
                    newUser.setLastName(names.length > 1 ? names[1] : "");
                    newUser.setPassword("oauth_" + provider); // Mot de passe fictif pour OAuth
                    newUser.setType(UserType.EMPLOYEE);
                    newUser.setLanguage("fr");
                    LocalDateTime now = LocalDateTime.now();
                    newUser.setCreationDate(now);
                    newUser.setUpdateDate(now);
                    return userRepository.save(newUser);
                });
    }
}

