package edu.lyra.members.api.parent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.person.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static java.util.Optional.ofNullable;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

/**
 * The parent role held by a {@link Person}, sharing that person's primary key and delegating its identity fields
 * ({@code name}, {@code surname}, {@code mail}) straight through to it.
 *
 * @author Esteban Cristóbal Rodríguez
 * @see Auditable
 * @see Person
 */
@Getter
@ToString
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "PARENTS")
public class Parent
        extends Auditable {

    @Setter
    @Getter(AccessLevel.NONE)
    @JsonIgnore
    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;
    @Setter
    @Valid
    @NotNull
    @JsonIgnore
    @MapsId
    @OneToOne(cascade = { PERSIST, MERGE })
    @JoinColumn(name = "ID")
    private Person person;

    @Builder
    private Parent(final UUID id, final Person person) {
        this.id     = id;
        this.person = person;
    }

    @Exclude
    @OneToMany(cascade = { PERSIST, MERGE })
    @JoinColumn(name = "PARENT_ID")
    private Set<Kid> kids = new HashSet<>();

    /**
     * @return the parent's id
     */
    public UUID getId() {
        final UUID personId = this.person != null ? this.person.getId() : null;
        return ofNullable(this.id).orElse(personId);
    }

    /**
     * @return the parent's given name
     */
    public String getName() {
        return this.person.getName();
    }

    /**
     * @param name the given name to set
     */
    public void setName(final String name) {
        this.person().setName(name);
    }

    private Person person() {
        if(this.person == null) {
            this.person = Person.builder().build();
        }
        return this.person;
    }

    /**
     * @return the parent's surname
     */
    public String getSurname() {
        return this.person.getSurname();
    }

    /**
     * @param surname the surname to set
     */
    public void setSurname(final String surname) {
        this.person().setSurname(surname);
    }

    /**
     * @return the parent's email address
     */
    public String getMail() {
        return this.person.getMail();
    }

    /**
     * @param mail the email address to set
     */
    public void setMail(final String mail) {
        this.person().setMail(mail);
    }

}
