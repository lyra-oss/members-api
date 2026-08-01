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

    // Jackson's default VisibilityChecker only auto-detects public getters, regardless of the
    // declaring class's own visibility - so these must stay public even though SchoolModel itself is
    // package-private (VerticalSliceRulesTest only forbids the class declaration from being public).
    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

}
