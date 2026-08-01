package edu.lyra.members.api.teacher.rest;

import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "teachers", itemRelation = "teacher")
class TeacherModel
        extends RepresentationModel<TeacherModel> {

    private final UUID id;

    private final String name;

    private final String surname;

    private final String mail;

    TeacherModel(final UUID id, final String name, final String surname, final String mail) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.mail = mail;
    }

    // Jackson's default VisibilityChecker only auto-detects public getters, regardless of the
    // declaring class's own visibility.
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
