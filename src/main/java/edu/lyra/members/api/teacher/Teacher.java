package edu.lyra.members.api.teacher;

import java.util.UUID;

import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRole;
import edu.lyra.members.api.school.School;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * The teacher role held by a {@link Person} at a {@link School}, sharing that person's primary key and delegating its
 * identity fields ({@code name}, {@code surname}, {@code mail}) straight through to it (see {@link PersonRole}).
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
@Table(name = "TEACHERS")
public class Teacher
        extends PersonRole {

    @NotNull
    @ManyToOne
    private School school;

    @Builder
    private Teacher(final UUID id, final Person person, final School school) {
        super(id, person);
        this.school = school;
    }

}
