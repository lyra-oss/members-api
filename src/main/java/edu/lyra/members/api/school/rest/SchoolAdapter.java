package edu.lyra.members.api.school.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.web.ApiBasePath;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

class SchoolAdapter
        implements RepresentationModelAssembler<School, SchoolModel> {

    private final SchoolRepository    repository;
    private final TeacherRepository   teacherRepository;
    private final ClassroomRepository classroomRepository;
    private final SchoolMapper        mapper;
    private final SchoolPolicy        policy;
    private final ApiBasePath         apiBasePath;

    SchoolAdapter(
            final SchoolRepository repository,
            final TeacherRepository teacherRepository,
            final ClassroomRepository classroomRepository,
            final SchoolMapper mapper,
            final SchoolPolicy policy,
            final ApiBasePath apiBasePath
    ) {
        this.repository          = repository;
        this.teacherRepository   = teacherRepository;
        this.classroomRepository = classroomRepository;
        this.mapper              = mapper;
        this.policy              = policy;
        this.apiBasePath         = apiBasePath;
    }

    @Override
    public SchoolModel toModel(final School school) {
        final SchoolModel model = this.mapper.toModel(school);
        model.add(this.selfLink(school));
        return model;
    }

    // WebMvcLinkBuilder.linkTo(methodOn(...)) builds its URI from the raw, unresolved text of
    // @RequestMapping's "${lyra.api.base-path}" placeholder rather than from the value Spring actually
    // resolves the mapping to at dispatch time, so links built that way come out broken
    // ("${lyra.api.base-path}/schools/..." verbatim). Building the link from ApiBasePath directly
    // sidesteps that gap entirely.
    private Link selfLink(final School school) {
        //@formatter:off
        final String href = ServletUriComponentsBuilder.fromCurrentContextPath()
                                                        .path(this.apiBasePath.basePath())
                                                        .path("/schools/{id}")
                                                        .buildAndExpand(school.getId())
                                                        .toUriString();
        //@formatter:on
        return Link.of(href).withSelfRel();
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
        return this.toModel(this.repository.save(school));
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
