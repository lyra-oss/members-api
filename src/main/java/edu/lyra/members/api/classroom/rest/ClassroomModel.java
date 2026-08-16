package edu.lyra.members.api.classroom.rest;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Relation(collectionRelation = "classrooms", itemRelation = "classroom")
class ClassroomModel
        extends RepresentationModel<ClassroomModel> {

    private final UUID id;

    private final int course;

    private final String group;

}
