package edu.lyra.members.api.school.rest;

import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "schools", itemRelation = "school")
class SchoolModel
        extends RepresentationModel<SchoolModel> {

    private final UUID id;

    private final String name;

    SchoolModel(final UUID id, final String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

}
