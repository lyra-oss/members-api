package edu.lyra.members.api.kid.rest;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Relation(collectionRelation = "kids", itemRelation = "kid")
class KidModel
        extends RepresentationModel<KidModel> {

    private final UUID id;

    private final String name;

    private final String surname;

    private final LocalDate birthdate;

}
