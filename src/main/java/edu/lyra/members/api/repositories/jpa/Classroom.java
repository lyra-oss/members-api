package edu.lyra.members.api.repositories.jpa;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static jakarta.persistence.CascadeType.ALL;

@Getter
@ToString
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "CLASSROOMS",
        uniqueConstraints = @UniqueConstraint(columnNames = { "COURSE", "GROUP_NAME", "SCHOOL_ID" })
)
public class Classroom
        extends Auditable {

    @JsonIgnore
    @Id
    @GeneratedValue(generator = "classrooms_seq")
    @SequenceGenerator(name = "classrooms_seq", sequenceName = "CLASSROOMS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private int id;

    @Positive
    @Max(6)
    @Column(name = "COURSE", length = 1, nullable = false)
    private int course;

    @Pattern(regexp = "^[A-Z]$")
    @Column(name = "GROUP_NAME", length = 1, nullable = false)
    private String group;

    @ManyToOne
    private School school;

    @Exclude
    @OneToMany(cascade = ALL)
    @JoinColumn(name = "CLASSROOM_ID")
    private Set<Kid> kids;

}
