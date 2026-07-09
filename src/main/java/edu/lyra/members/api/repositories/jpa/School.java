package edu.lyra.members.api.repositories.jpa;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Table(name = "SCHOOLS")
public class School
        extends Auditable {

    @JsonIgnore
    @Id
    @GeneratedValue(generator = "schools_seq")
    @SequenceGenerator(name = "schools_seq", sequenceName = "SCHOOLS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private int id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @Exclude
    @OneToMany(cascade = ALL)
    @JoinColumn(name = "SCHOOL_ID")
    private Set<Classroom> classrooms;

}
