package edu.lyra.members.api.repositories.jpa;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "TEACHERS")
public class Teacher
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

    @NotNull
    @ManyToOne
    private School school;

}
