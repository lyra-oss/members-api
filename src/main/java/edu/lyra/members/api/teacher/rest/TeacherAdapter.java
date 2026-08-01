package edu.lyra.members.api.teacher.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.config.web.ApiBasePath;
import edu.lyra.members.api.exceptions.UnresolvableReferenceException;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
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

class TeacherAdapter
        implements RepresentationModelAssembler<Teacher, TeacherModel> {

    private final TeacherRepository teacherRepository;
    private final SchoolRepository  schoolRepository;
    private final PersonRepository  personRepository;
    private final TeacherMapper     mapper;
    private final TeacherPolicy     policy;
    private final ApiBasePath       apiBasePath;

    TeacherAdapter(
            final TeacherRepository teacherRepository,
            final SchoolRepository schoolRepository,
            final PersonRepository personRepository,
            final TeacherMapper mapper,
            final TeacherPolicy policy,
            final ApiBasePath apiBasePath
    ) {
        this.teacherRepository = teacherRepository;
        this.schoolRepository  = schoolRepository;
        this.personRepository  = personRepository;
        this.mapper            = mapper;
        this.policy            = policy;
        this.apiBasePath       = apiBasePath;
    }

    @Override
    public TeacherModel toModel(final Teacher teacher) {
        final TeacherModel model = this.mapper.toModel(teacher);
        model.add(this.selfLink(teacher));
        return model;
    }

    private Link selfLink(final Teacher teacher) {
        //@formatter:off
        final String href = ServletUriComponentsBuilder.fromCurrentContextPath()
                                                        .path(this.apiBasePath.basePath())
                                                        .path("/teachers/{id}")
                                                        .buildAndExpand(teacher.getId())
                                                        .toUriString();
        //@formatter:on
        return Link.of(href).withSelfRel();
    }

    Optional<TeacherModel> findById(final UUID id) {
        return this.teacherRepository.findById(id).map(this::toModel);
    }

    PagedModel<TeacherModel> findAll(final Pageable pageable, final PagedResourcesAssembler<Teacher> pagedAssembler) {
        final Page<Teacher> page = this.teacherRepository.findAll(pageable);
        return pagedAssembler.toModel(page, this);
    }

    // Mirrors the original TeacherRegistrationHandler: if the authenticated subject already has a
    // Person record (e.g. they registered as a parent first), that existing identity wins over
    // whatever name/surname/mail this request supplied; otherwise a new Person is created from the
    // request under the subject's id.
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
        return this.toModel(this.teacherRepository.save(teacher));
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
