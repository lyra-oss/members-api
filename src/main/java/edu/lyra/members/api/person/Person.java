package edu.lyra.members.api.person;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.lyra.members.api.config.jpa.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * The identity of a human known to Lyra, independent of the roles ({@code Parent}, {@code Teacher}) they perform. Its
 * {@link #id} is the identity provider's subject claim, shared as the primary key of every role a person holds (see
 * {@code Parent}/{@code Teacher}, which map their own {@code @Id} onto this one via {@code @MapsId}).
 */
@Getter
@ToString
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "PERSONS")
public class Person
        extends Auditable {

    @Setter
    @JsonIgnore
    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;
    @Setter
    @NotBlank
    @Size(max = 100)
    @Column(name = "NAME", length = 100, nullable = false)
    private String name;
    @Setter
    @NotBlank
    @Size(max = 100)
    @Column(name = "SURNAME", length = 100, nullable = false)
    private String surname;
    @Setter
    @Email
    @NotBlank
    @Size(max = 200)
    @Column(name = "EMAIL", length = 200, nullable = false, unique = true)
    private String mail;

    @Builder
    private Person(final UUID id, final String name, final String surname, final String mail) {
        this.id      = id;
        this.name    = name;
        this.surname = surname;
        this.mail    = mail;
    }

}
