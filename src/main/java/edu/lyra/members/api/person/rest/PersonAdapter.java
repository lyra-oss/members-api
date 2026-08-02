package edu.lyra.members.api.person.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.exceptions.ParentHasKidsException;
import edu.lyra.members.api.exceptions.TeacherAssignedToClassroomException;
import edu.lyra.members.api.exceptions.UnresolvableReferenceException;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
class PersonAdapter
        implements RepresentationModelAssembler<Person, PersonModel> {

    private final PersonRepository    personRepository;
    private final ParentRepository    parentRepository;
    private final TeacherRepository   teacherRepository;
    private final SchoolRepository    schoolRepository;
    private final ClassroomRepository classroomRepository;
    private final PersonMapper        mapper;

    PersonAdapter(
            final PersonRepository personRepository,
            final ParentRepository parentRepository,
            final TeacherRepository teacherRepository,
            final SchoolRepository schoolRepository,
            final ClassroomRepository classroomRepository,
            final PersonMapper mapper
    ) {
        this.personRepository    = personRepository;
        this.parentRepository    = parentRepository;
        this.teacherRepository   = teacherRepository;
        this.schoolRepository    = schoolRepository;
        this.classroomRepository = classroomRepository;
        this.mapper              = mapper;
    }

    @Override
    public PersonModel toModel(final Person person) {
        final PersonModel model = this.mapper.toModel(person);
        model.add(linkTo(methodOn(PersonController.class).get(person.getId())).withSelfRel());
        return model;
    }

    Optional<PersonModel> findById(final UUID id) {
        return this.personRepository.findById(id).map(this::toModel);
    }

    PagedModel<PersonModel> findAll(final Pageable pageable, final PagedResourcesAssembler<Person> pagedAssembler) {
        final Page<Person> page = this.personRepository.findAll(pageable);
        return pagedAssembler.toModel(page, this);
    }

    boolean grantParentRole(final UUID id) {
        return this.personRepository.findById(id).map(person -> {
            if(this.parentRepository.existsById(id)) {
                log.debug("Person {} is already a parent; leaving the parent role unchanged", id);
            } else {
                this.parentRepository.save(Parent.builder().person(person).build());
                log.debug("Granted the parent role to person {}", id);
            }
            return true;
        }).orElse(false);
    }

    boolean grantTeacherRole(final UUID id, final GrantTeacherRoleRequest request) {
        final Optional<Person> person = this.personRepository.findById(id);
        if(person.isEmpty()) {
            return false;
        }
        if(this.teacherRepository.existsById(id)) {
            log.debug("Person {} is already a teacher; leaving the teacher role unchanged", id);
            return true;
        }
        final School school = this.schoolRepository.findById(request.school()).orElseThrow(
                () -> new UnresolvableReferenceException("No school found with id " + request.school()));
        this.teacherRepository.save(Teacher.builder().person(person.get()).school(school).build());
        log.debug("Granted the teacher role to person {} at school {}", id, school.getId());
        return true;
    }

    boolean revokeParentRole(final UUID id) {
        return this.parentRepository.findById(id).map(parent -> {
            if(! parent.getKids().isEmpty()) {
                log.debug("Parent {} still has kids; refusing to revoke the parent role", id);
                throw new ParentHasKidsException(
                        ("Parent %s still has %d kid(s) linked; remove or reassign them before revoking the parent " +
                         "role").formatted(id, parent.getKids().size()));
            }
            this.parentRepository.delete(parent);
            log.debug("Revoked the parent role from person {}", id);
            return true;
        }).orElse(false);
    }

    boolean revokeTeacherRole(final UUID id) {
        return this.teacherRepository.findById(id).map(teacher -> {
            if(this.classroomRepository.existsByTutorIdOrTeachersId(id)) {
                log.debug("Teacher {} is still referenced by a classroom; refusing to revoke the teacher role", id);
                throw new TeacherAssignedToClassroomException(
                        ("Teacher %s still tutors or teaches at least one classroom; unassign them before revoking " +
                         "the teacher role").formatted(id));
            }
            this.teacherRepository.delete(teacher);
            log.debug("Revoked the teacher role from person {}", id);
            return true;
        }).orElse(false);
    }

}
