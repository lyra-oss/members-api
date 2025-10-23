package com.sagittec.lyra.members.api.repositories;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.annotation.LastModifiedDate;

@NoArgsConstructor
@Getter
@ToString
@Entity
@Table(name = "PARENTS")
public class Parent {

    @Id
    @GeneratedValue(generator = "parents_seq")
    @SequenceGenerator(name = "parents_seq", sequenceName = "PARENTS_SEQ", allocationSize = 1)
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

    @Email
    @NotBlank
    @Size(max = 200)
    @Column(name = "EMAIL", length = 200, nullable = false, unique = true)
    private String mail;

    @Setter
    @Exclude
    @OneToMany(mappedBy = "parent")
    private Set<Kid> kids;

    @Version
    @Column(name = "OPTLOCK")
    private int version;

    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @Builder
    private Parent(final String name, final String surname, final String mail) {
        this.name    = name;
        this.surname = surname;
        this.mail    = mail;
    }

}
