package edu.lyra.members.api.repositories.jpa;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static jakarta.persistence.CascadeType.ALL;

@Getter
@ToString
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "PARENTS")
public class Parent
        extends Auditable {

    @Setter
    @JsonIgnore
    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;

    @Valid
    @JsonUnwrapped
    @Embedded
    private ContactInfo contactInfo;

    @Builder
    private Parent(final UUID id, final ContactInfo contactInfo) {
        this.id          = id;
        this.contactInfo = contactInfo;
    }

    @Exclude
    @OneToMany(cascade = ALL)
    @JoinColumn(name = "PARENT_ID")
    private Set<Kid> kids;

}
