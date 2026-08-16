package edu.lyra.members.api.teacher.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.UnresolvableReferenceException;
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
class TeacherAdapter
        implements RepresentationModelAssembler<Teacher, TeacherModel> {

    private final TeacherRepository   teacherRepository;
    private final SchoolRepository    schoolRepository;
    private final PersonRepository    personRepository;
    private final ClassroomRepository classroomRepository;
    private final TeacherMapper       mapper;
    private final TeacherPolicy       policy;

    TeacherAdapter(
            final TeacherRepository teacherRepository,
            final SchoolRepository schoolRepository,
            final PersonRepository personRepository,
            final ClassroomRepository classroomRepository,
            final TeacherMapper mapper,
            final TeacherPolicy policy
    ) {
        this.teacherRepository   = teacherRepository;
        this.schoolRepository    = schoolRepository;
        this.personRepository    = personRepository;
        this.classroomRepository = classroomRepository;
        this.mapper              = mapper;
        this.policy              = policy;
    }

    Optional<TeacherModel> findById(final UUID id) {
        return this.teacherRepository.findById(id).map(this::toModel);
    }

    PagedModel<TeacherModel> findAll(final Pageable pageable, final PagedResourcesAssembler<Teacher> pagedAssembler) {
        final Page<Teacher> page = this.teacherRepository.findAll(pageable);
        return pagedAssembler.toModel(page, this);
    }

    Optional<PagedModel<TeacherModel>> findBySchool(
            final UUID schoolId,
            final Pageable pageable,
            final PagedResourcesAssembler<Teacher> pagedAssembler
    ) {
        if(! this.schoolRepository.existsById(schoolId)) {
            return Optional.empty();
        }
        final Page<Teacher> page = this.teacherRepository.findBySchoolId(schoolId, pageable);
        return Optional.of(pagedAssembler.toModel(page, this));
    }

    Optional<PagedModel<TeacherModel>> findByClassroom(
            final UUID classroomId,
            final Pageable pageable,
            final PagedResourcesAssembler<Teacher> pagedAssembler
    ) {
        if(! this.classroomRepository.existsById(classroomId)) {
            return Optional.empty();
        }
        final Page<Teacher> page = this.teacherRepository.findByClassroomId(classroomId, pageable);
        return Optional.of(pagedAssembler.toModel(page, this));
    }

    Optional<TeacherModel> findTutorOf(final UUID classroomId) {
        return this.classroomRepository.findById(classroomId).map(Classroom::getTutor).map(this::toModel);
    }

    TeacherModel create(final TeacherRequest request) {
        final School school = this.schoolRepository.findById(request.school()).orElseThrow(
                () -> new UnresolvableReferenceException("No school found with id " + request.school()));
        final Teacher teacher = this.mapper.toEntity(request, school);
        final UUID    subject = AuthenticatedPrincipal.requireCurrentId();
        final Person person = this.personRepository.findById(subject).orElseGet(() -> {
            final Person newPerson = teacher.getPerson();
            newPerson.setId(subject);
            return newPerson;
        });
        teacher.setPerson(person);
        final Teacher saved = this.teacherRepository.save(teacher);
        log.debug("Created teacher {} at school {}", saved.getId(), school.getId());
        return this.toModel(saved);
    }

    @Override
    public TeacherModel toModel(final Teacher teacher) {
        final TeacherModel model = this.mapper.toModel(teacher);
        model.add(linkTo(methodOn(TeacherController.class).get(teacher.getId())).withSelfRel());
        return model;
    }

    Optional<TeacherModel> update(final UUID id, final TeacherPatchRequest request) {
        return this.teacherRepository.findById(id).map(teacher -> {
            this.policy.authorizeUpdate(teacher);
            this.mapper.update(request, teacher);
            return this.toModel(this.teacherRepository.save(teacher));
        });
    }

    boolean delete(final UUID id) {
        final Optional<Teacher> found = this.teacherRepository.findById(id);
        if(found.isEmpty()) {
            return false;
        }
        final Teacher teacher = found.get();
        this.policy.authorizeDelete(teacher);
        this.teacherRepository.delete(teacher);
        return true;
    }

}
