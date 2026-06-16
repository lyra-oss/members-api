package edu.lyra.members.api.repositories.jpa;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
@Table(name = "PARENTS")
public class Parent {

    @JsonIgnore
    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;

    @Builder
    private Parent(final UUID id, final String name, final String surname, final String mail) {
        this.id = id;
        this.name    = name;
        this.surname = surname;
        this.mail    = mail;
    }

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

    @Exclude
    @OneToMany(cascade = ALL)
    @JoinColumn(name = "PARENT_ID")
    private Set<Kid> kids;

    @JsonIgnore
    @Version
    @Column(name = "OPTLOCK")
    private int version;

    @JsonIgnore
    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    public void setId(final UUID id) {
        this.id = id;
    }

}
