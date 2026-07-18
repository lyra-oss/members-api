package edu.lyra.members.api.person;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.lyra.members.api.config.jpa.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static java.util.Optional.ofNullable;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

/**
 * Base class for the roles ({@code Parent}, {@code Teacher}) a {@link Person} can hold, sharing that person's primary
 * key via {@code @MapsId} and delegating its identity fields ({@code name}, {@code surname}, {@code mail}) straight
 * through to it.
 *
 * @author Esteban Cristóbal Rodríguez
 * @see Auditable
 * @see Person
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class PersonRole
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

    protected PersonRole(final UUID id, final Person person) {
        this.id     = id;
        this.person = person;
    }

    /**
     * @return the role holder's id
     */
    public UUID getId() {
        final UUID personId = this.person != null ? this.person.getId() : null;
        return ofNullable(this.id).orElse(personId);
    }

    /**
     * @return the role holder's given name
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
     * @return the role holder's surname
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
     * @return the role holder's email address
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
