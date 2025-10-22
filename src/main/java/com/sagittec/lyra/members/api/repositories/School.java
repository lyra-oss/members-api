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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.annotation.LastModifiedDate;

@NoArgsConstructor
@Getter
@ToString
@Entity
@Table(name = "SCHOOLS")
public class School {

    @Builder
    private School(final String name) {
        this.name = name;
    }

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
    @OneToMany(mappedBy = "school")
    private Set<Classroom> classrooms;

    @Version
    @Column(name = "OPTLOCK")
    private int version;

    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    private LocalDateTime lastModifiedDate;

}
