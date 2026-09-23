package edu.lyra.members.api.parent;

import java.util.UUID;

import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

    @Builder
    private Parent(final UUID id, final Person person) {
        super(id, person);
    }

}
