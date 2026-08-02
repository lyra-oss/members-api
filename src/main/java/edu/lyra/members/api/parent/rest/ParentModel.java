package edu.lyra.members.api.parent.rest;

import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "parents", itemRelation = "parent")
class ParentModel
        extends RepresentationModel<ParentModel> {

    private final UUID id;

    private final String name;

    private final String surname;

    private final String mail;

    ParentModel(final UUID id, final String name, final String surname, final String mail) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.mail = mail;
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getSurname() {
        return this.surname;
    }

    public String getMail() {
        return this.mail;
    }

}
