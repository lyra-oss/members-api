package com.sagittec.lyra.members.api.repositories;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "KIDS")
class Kid {

    @Id
    @GeneratedValue(generator = "kids_seq")
    @SequenceGenerator(name = "kids_seq", sequenceName = "KIDS_SEQ", allocationSize = 1, initialValue = 1)
    @Column(name = "ID")
    private int id;

    @Version
    @Column(name = "OPTLOCK")
    @Setter(AccessLevel.NONE)
    private int version;

    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    @Setter(AccessLevel.NONE)
    private LocalDateTime lastModifiedDate;

}
