package edu.lyra.members.api.repositories.jpa;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static jakarta.persistence.CascadeType.ALL;

@Getter
@ToString
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "SCHOOLS")
public class School {

    @JsonIgnore
    @Id
    @GeneratedValue(generator = "schools_seq")
    @SequenceGenerator(name = "schools_seq", sequenceName = "SCHOOLS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private int id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @Exclude
    @OneToMany(cascade = ALL)
    @JoinColumn(name = "SCHOOL_ID")
    private Set<Classroom> classrooms;

    @JsonIgnore
    @Version
    @Column(name = "OPTLOCK")
    private int version;

    @JsonIgnore
    @CreatedDate
    @Column(name = "CREATED_DATE", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @JsonIgnore
    @CreatedBy
    @Column(name = "CREATED_BY", length = 100, nullable = false, updatable = false)
    private String createdBy;

    @JsonIgnore
    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

    @JsonIgnore
    @LastModifiedBy
    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

}
