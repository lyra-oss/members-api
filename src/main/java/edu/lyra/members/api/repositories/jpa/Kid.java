package edu.lyra.members.api.repositories.jpa;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "KIDS", uniqueConstraints = @UniqueConstraint(columnNames = { "NAME", "BIRTHDATE", "PARENT_ID" }))
public class Kid {

    @Builder
    private Kid(final String name, final String surname, final LocalDate birthdate) {
        this.name      = name;
        this.surname   = surname;
        this.birthdate = birthdate;
    }

    @JsonIgnore
    @Id
    @GeneratedValue(generator = "kids_seq")
    @SequenceGenerator(name = "kids_seq", sequenceName = "KIDS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private int id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(name = "SURNAME", length = 100, nullable = false)
    private String surname;

    @Past
    @NotNull
    @Column(name = "BIRTHDATE", nullable = false)
    private LocalDate birthdate;

    @ManyToOne
    private Parent parent;

    @ManyToOne
    private Classroom classroom;

    @JsonIgnore
    @Version
    @Column(name = "OPTLOCK")
    private int version;

    @JsonIgnore
    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

}
