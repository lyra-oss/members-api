package com.sagittec.lyra.members.api.repositories;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.annotation.LastModifiedDate;

import static jakarta.persistence.CascadeType.ALL;

@Getter
@ToString
@NoArgsConstructor
@Entity
@Table(
        name = "CLASSROOMS",
        uniqueConstraints = @UniqueConstraint(columnNames = { "COURSE", "GROUP_NAME", "SCHOOL_ID" })
)
public class Classroom {

    @Builder
    private Classroom(final int course, final String group, final School school) {
        this.course = course;
        this.group  = group;
        this.school = school;
    }

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

    @JsonIgnore
    @Version
    @Column(name = "OPTLOCK")
    private int version;

    @JsonIgnore
    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

}
