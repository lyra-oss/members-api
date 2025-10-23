package com.sagittec.lyra.members.api.repositories;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.LastModifiedDate;

@NoArgsConstructor
@Getter
@ToString
@Entity
@Table(name = "KIDS")
public class Kid {

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
    @Column(name = "BIRTHDATE")
    private LocalDate birthdate;

    @ManyToOne
    @JoinColumn(name = "PARENT_ID")
    private Parent parent;

    @ManyToOne
    @JoinColumn(name = "CLASSROOM_ID")
    private Classroom classroom;

    @Version
    @Column(name = "OPTLOCK")
    @Setter(AccessLevel.NONE)
    private int version;

    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    @Setter(AccessLevel.NONE)
    private LocalDateTime lastModifiedDate;

    @Builder
    private Kid(final String name, final String surname, final LocalDate birthdate) {
        this.name      = name;
        this.surname   = surname;
        this.birthdate = birthdate;
    }

}
