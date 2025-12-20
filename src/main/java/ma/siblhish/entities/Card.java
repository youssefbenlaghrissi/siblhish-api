package ma.siblhish.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code; // Identifiant unique: "bar_chart", "pie_chart", etc.
    
    @Column(name = "title", nullable = false, length = 200)
    private String title; // Titre de la carte

}

