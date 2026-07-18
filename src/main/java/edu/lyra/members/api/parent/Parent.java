package edu.lyra.members.api.parent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

/**
 * The parent role held by a {@link Person}, sharing that person's primary key and delegating its identity fields
 * ({@code name}, {@code surname}, {@code mail}) straight through to it (see {@link PersonRole}).
 *
 * @author Esteban Cristóbal Rodríguez
 * @see Auditable
 * @see Person
 */
@Getter
@ToString(callSuper = true)
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "PARENTS")
public class Parent
        extends PersonRole {

    @Exclude
    @OneToMany(cascade = { PERSIST, MERGE })
    @JoinColumn(name = "PARENT_ID")
    private Set<Kid> kids = new HashSet<>();

    @Builder
    private Parent(final UUID id, final Person person) {
        super(id, person);
    }

}
