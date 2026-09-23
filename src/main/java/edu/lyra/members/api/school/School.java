package edu.lyra.members.api.school;

import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.teacher.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A school, the shared organizational anchor for its classrooms and teachers.
 *
 * <p>Those {@link Classroom}s and {@link Teacher}s are deliberately not mapped as collections here: they are reached
 * through {@code ClassroomRepository} and {@code TeacherRepository}, which page and order them, instead of being loaded
 * whole into this entity.
 *
 * @author Esteban Cristóbal Rodríguez
 * @see Auditable
 */
@Getter
@ToString
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "SCHOOLS")
public class School
        extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Setter
    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

}
