package edu.lyra.members.api.kid;

import java.time.LocalDate;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.parent.Parent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A child enrolled at Lyra, linked to a {@link Parent} and optionally a {@link Classroom}.
 *
 * @author Esteban Cristóbal Rodríguez
 * @see Auditable
 */
@Getter
@ToString
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "KIDS",
        // PARENT_ID leads so the constraint's own index also serves the lookups that filter on it. The set of
        // columns is what makes the constraint, not their order, so this costs nothing and saves an index:
        // findByParentIdOrderByNameAsc reads it in NAME order without a sort, and countByParentId ranges over it.
        uniqueConstraints = @UniqueConstraint(columnNames = { "PARENT_ID", "NAME", "BIRTHDATE" }),
        indexes = @Index(name = "IDX_KIDS_CLASSROOM_ID", columnList = "CLASSROOM_ID")
)
public class Kid
        extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Setter
    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @Setter
    @Column(name = "SURNAME", length = 100, nullable = false)
    private String surname;

    @Setter
    @Column(name = "BIRTHDATE", nullable = false)
    private LocalDate birthdate;

    @Setter
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    private Parent parent;

    @Setter
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    private Classroom classroom;

}
