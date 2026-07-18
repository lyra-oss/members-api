package edu.lyra.members.api.school;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.teacher.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

@Getter
@ToString
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "SCHOOLS")
public class School
        extends Auditable {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    /**
     * Deliberately excludes {@link jakarta.persistence.CascadeType#REMOVE}: a school with classrooms or teachers still
     * linked must not be deletable at all (see delete-authorization handlers), so the generated
     * {@code CLASSROOMS.SCHOOL_ID}/{@code TEACHERS.SCHOOL_ID} foreign keys are left to enforce that at the database
     * level rather than having Hibernate cascade the deletion away.
     */
    @Exclude
    @OneToMany(cascade = { PERSIST, MERGE })
    @JoinColumn(name = "SCHOOL_ID")
    private Set<Classroom> classrooms = new HashSet<>();

    @Exclude
    @OneToMany(mappedBy = "school", cascade = { PERSIST, MERGE })
    private Set<Teacher> teachers = new HashSet<>();

}
