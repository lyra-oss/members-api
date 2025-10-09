package com.sagittec.lyra.members.api.repositories;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
    private UUID id;

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    @LastModifiedDate
    @Setter(AccessLevel.NONE)
    private LocalDateTime lastModifiedDate;

}
