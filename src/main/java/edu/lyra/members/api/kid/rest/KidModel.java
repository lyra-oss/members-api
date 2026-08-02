package edu.lyra.members.api.kid.rest;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "kids", itemRelation = "kid")
class KidModel
        extends RepresentationModel<KidModel> {

    private final UUID id;

    private final String name;

    private final String surname;

    private final LocalDate birthdate;

    KidModel(final UUID id, final String name, final String surname, final LocalDate birthdate) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.birthdate = birthdate;
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

    public LocalDate getBirthdate() {
        return this.birthdate;
    }

}
