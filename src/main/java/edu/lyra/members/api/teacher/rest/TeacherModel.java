package edu.lyra.members.api.teacher.rest;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Relation(collectionRelation = "teachers", itemRelation = "teacher")
class TeacherModel
        extends RepresentationModel<TeacherModel> {

    private final UUID id;

    private final String name;

    private final String surname;

    private final String mail;

}
