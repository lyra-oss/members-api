package edu.lyra.members.api.classroom.rest;

import java.util.UUID;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "classrooms", itemRelation = "classroom")
class ClassroomModel
        extends RepresentationModel<ClassroomModel> {

    private final UUID id;

    private final int course;

    private final String group;

    ClassroomModel(final UUID id, final int course, final String group) {
        this.id = id;
        this.course = course;
        this.group = group;
    }

    // Jackson's default VisibilityChecker only auto-detects public getters, regardless of the
    // declaring class's own visibility.
    public UUID getId() {
        return this.id;
    }

    public int getCourse() {
        return this.course;
    }

    public String getGroup() {
        return this.group;
    }

}
