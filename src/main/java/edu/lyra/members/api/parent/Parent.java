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

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

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
    @OneToMany(cascade = ALL)
    @JoinColumn(name = "PARENT_ID")
    private Set<Kid> kids = new HashSet<>();

    public UUID getId() {
        final UUID personId = this.person != null ? this.person.getId() : null;
        return ofNullable(this.id).orElse(personId);
    }

    public String getName() {
        return this.person.getName();
    }

    public void setName(final String name) {
        this.person().setName(name);
    }

    private Person person() {
        if(this.person == null) {
            this.person = Person.builder().build();
        }
        return this.person;
    }

    public String getSurname() {
        return this.person.getSurname();
    }

    public void setSurname(final String surname) {
        this.person().setSurname(surname);
    }

    public String getMail() {
        return this.person.getMail();
    }

    public void setMail(final String mail) {
        this.person().setMail(mail);
    }

}
