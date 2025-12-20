package ma.siblhish.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "favoris")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Favorite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "type", nullable = false, length = 50)
    private String type; // Ex: "CARD", "CATEGORY_COLOR", etc.
    
    @Column(name = "target_entity", nullable = false)
    private Long targetEntity; // ID de l'entité ciblée (ex: ID de la carte, ID de la catégorie)
    
    @Column(name = "value", columnDefinition = "TEXT")
    private String value; // Ex: "position=1" pour les cartes, "#FF0000" pour les couleurs
}

