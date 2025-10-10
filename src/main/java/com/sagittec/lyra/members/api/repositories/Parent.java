package com.sagittec.lyra.members.api.repositories;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PARENTS")
class Parent {

    @Id
    @GeneratedValue(generator = "parents_seq")
    @SequenceGenerator(name = "parents_seq", sequenceName = "PARENTS_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private int id;

    @Size(max = 100)
    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @Size(max = 100)
    @Column(name = "SURNAME", length = 100, nullable = false)
    private String surname;

    @Email
    @Size(max = 200)
    @Column(name = "E_MAIL", length = 200, nullable = false)
    private String mail;

    @Exclude
    @OneToMany(mappedBy = "parent")
    private List<Kid> kids;

    @Version
    @Column(name = "OPTLOCK")
    @Setter(AccessLevel.NONE)
    private int version;

    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    @Setter(AccessLevel.NONE)
    private LocalDateTime lastModifiedDate;

}
