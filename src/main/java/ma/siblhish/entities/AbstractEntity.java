package ma.siblhish.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creation_date", updatable = true)
    private LocalDateTime creationDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;
}
