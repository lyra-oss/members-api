package edu.lyra.members.api.classroom;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A single course/group at a {@link School}, with an optional tutor and a teaching staff.
 *
 * <p>The enrolled {@code Kid}s are deliberately not mapped as a collection here: they are reached through
 * {@code KidRepository}, which pages and orders them, instead of being loaded whole into this entity.
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
        name = "CLASSROOMS",
        // SCHOOL_ID leads for the same reason as KIDS: the constraint is unchanged, and its index now serves
        // findBySchoolIdOrderByCourseAscGroupAsc and countBySchoolId instead of needing one of their own: the
        // first reads it in COURSE, GROUP_NAME order without a sort.
        uniqueConstraints = @UniqueConstraint(columnNames = { "SCHOOL_ID", "COURSE", "GROUP_NAME" }),
        indexes = @Index(name = "IDX_CLASSROOMS_TUTOR_ID", columnList = "TUTOR_ID")
)
public class Classroom
        extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @Setter
    @Column(name = "COURSE", length = 1, nullable = false)
    private int course;

    @Setter
    @Column(name = "GROUP_NAME", length = 1, nullable = false)
    private String group;

    @Setter
    @Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    private School school;

    @Setter
    @Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    private Teacher tutor;

    @Exclude
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "CLASSROOM_TEACHERS",
            joinColumns = @JoinColumn(name = "CLASSROOM_ID"),
            inverseJoinColumns = @JoinColumn(name = "TEACHER_ID"),
            // The join table's primary key is (CLASSROOM_ID, TEACHER_ID), so lookups by classroom already have an
            // index. Reaching the other way - which classrooms a teacher is on - has to be indexed separately.
            indexes = @Index(name = "IDX_CLASSROOM_TEACHERS_TEACHER_ID", columnList = "TEACHER_ID")
    )
    private Set<Teacher> teachers = new HashSet<>();

}
