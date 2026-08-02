package edu.lyra.members.api.school.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
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
class SchoolAdapter
        implements RepresentationModelAssembler<School, SchoolModel> {

    private final SchoolRepository    repository;
    private final TeacherRepository   teacherRepository;
    private final ClassroomRepository classroomRepository;
    private final SchoolMapper        mapper;
    private final SchoolPolicy        policy;

    SchoolAdapter(
            final SchoolRepository repository,
            final TeacherRepository teacherRepository,
            final ClassroomRepository classroomRepository,
            final SchoolMapper mapper,
            final SchoolPolicy policy
    ) {
        this.repository          = repository;
        this.teacherRepository   = teacherRepository;
        this.classroomRepository = classroomRepository;
        this.mapper              = mapper;
        this.policy              = policy;
    }

    @Override
    public SchoolModel toModel(final School school) {
        final SchoolModel model = this.mapper.toModel(school);
        model.add(linkTo(methodOn(SchoolController.class).get(school.getId())).withSelfRel());
        return model;
    }

    Optional<SchoolModel> findById(final UUID id) {
        return this.repository.findById(id).map(this::toModel);
    }

    PagedModel<SchoolModel> findAll(final Pageable pageable, final PagedResourcesAssembler<School> pagedAssembler) {
        final Page<School> page = this.repository.findAll(pageable);
        return pagedAssembler.toModel(page, this);
    }

    Optional<SchoolModel> findByTeacher(final UUID teacherId) {
        return this.teacherRepository.findById(teacherId).map(Teacher::getSchool).map(this::toModel);
    }

    Optional<SchoolModel> findByClassroom(final UUID classroomId) {
        return this.classroomRepository.findById(classroomId).map(Classroom::getSchool).map(this::toModel);
    }

    SchoolModel create(final SchoolRequest request) {
        final School school = this.mapper.toEntity(request);
        final School saved  = this.repository.save(school);
        log.debug("Created school {}", saved.getId());
        return this.toModel(saved);
    }

    Optional<SchoolModel> update(final UUID id, final SchoolRequest request) {
        return this.repository.findById(id).map(school -> {
            this.policy.authorizeUpdate(school);
            this.mapper.update(request, school);
            return this.toModel(this.repository.save(school));
        });
    }

    boolean delete(final UUID id) {
        final Optional<School> found = this.repository.findById(id);
        if(found.isEmpty()) {
            return false;
        }
        final School school = found.get();
        this.policy.authorizeDelete(school);
        this.repository.delete(school);
        return true;
    }

}
