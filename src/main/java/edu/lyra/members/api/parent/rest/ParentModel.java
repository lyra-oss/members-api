package edu.lyra.members.api.parent.rest;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Relation(collectionRelation = "parents", itemRelation = "parent")
class ParentModel
        extends RepresentationModel<ParentModel> {

    private final UUID id;

    private final String name;

    private final String surname;

    private final String mail;

}
