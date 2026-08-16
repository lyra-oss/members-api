package edu.lyra.members.api.classroom.rest;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.exceptions.SchoolMismatchException;
import edu.lyra.members.api.exceptions.UnresolvableReferenceException;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RequiredArgsConstructor
class ClassroomAdapter
        implements RepresentationModelAssembler<Classroom, ClassroomModel> {

    private final ClassroomRepository classroomRepository;
    private final SchoolRepository    schoolRepository;
    private final TeacherRepository   teacherRepository;
    private final KidRepository       kidRepository;
    private final ClassroomMapper     mapper;
    private final ClassroomPolicy     policy;

    Optional<ClassroomModel> findById(final UUID id) {
        return this.classroomRepository.findById(id).map(this::toModel);
    }

    PagedModel<ClassroomModel> findAll(
            final Pageable pageable,
            final PagedResourcesAssembler<Classroom> pagedAssembler
    ) {
        final Page<Classroom> page = this.classroomRepository.findAll(pageable);
        return pagedAssembler.toModel(page, this);
    }

    Optional<ClassroomModel> findByKid(final UUID kidId) {
        return this.kidRepository.findById(kidId).map(Kid::getClassroom).map(this::toModel);
    }

    Optional<PagedModel<ClassroomModel>> findBySchool(
            final UUID schoolId,
            final Pageable pageable,
            final PagedResourcesAssembler<Classroom> pagedAssembler
    ) {
        if(! this.schoolRepository.existsById(schoolId)) {
            return Optional.empty();
        }
        final Page<Classroom> page = this.classroomRepository.findBySchoolId(schoolId, pageable);
        return Optional.of(pagedAssembler.toModel(page, this));
    }

    ClassroomModel create(final ClassroomRequest request) {
        final School school = this.schoolRepository.findById(request.school()).orElseThrow(
                () -> new UnresolvableReferenceException("No school found with id " + request.school()));
        final Teacher tutor = this.resolveTutor(request.tutor());
        this.verifySchoolMembership(school, tutor);
        final Classroom classroom = this.mapper.toEntity(request, school, tutor);
        final Classroom saved     = this.classroomRepository.save(classroom);
        log.debug("Created classroom {} at school {}", saved.getId(), school.getId());
        return this.toModel(saved);
    }

    private Teacher resolveTutor(final UUID tutorId) {
        if(tutorId == null) {
            return null;
        }
        return this.teacherRepository.findById(tutorId).orElseThrow(
                () -> new UnresolvableReferenceException("No teacher found with id " + tutorId));
    }

    private void verifySchoolMembership(final School school, final Teacher teacher) {
        if(teacher != null && ! school.getId().equals(teacher.getSchool().getId())) {
            throw new SchoolMismatchException(
                    "Teacher %s does not belong to the classroom's school".formatted(teacher.getId()));
        }
    }

    @Override
    public ClassroomModel toModel(final Classroom classroom) {
        final ClassroomModel model = this.mapper.toModel(classroom);
        model.add(linkTo(methodOn(ClassroomController.class).get(classroom.getId())).withSelfRel());
        return model;
    }

    Optional<ClassroomModel> update(final UUID id, final ClassroomPatchRequest request) {
        return this.classroomRepository.findById(id).map(classroom -> {
            this.policy.authorizeUpdate(classroom);
            this.mapper.update(request, classroom);
            return this.toModel(this.classroomRepository.save(classroom));
        });
    }

    boolean delete(final UUID id) {
        final Optional<Classroom> found = this.classroomRepository.findById(id);
        if(found.isEmpty()) {
            return false;
        }
        final Classroom classroom = found.get();
        this.policy.authorizeDelete(classroom);
        this.classroomRepository.delete(classroom);
        return true;
    }

    boolean addTeacher(final UUID classroomId, final UUID teacherId) {
        return this.linkTeacher(classroomId, teacherId, (classroom, teacher) -> classroom.getTeachers().add(teacher));
    }

    boolean setTutor(final UUID classroomId, final UUID teacherId) {
        return this.linkTeacher(classroomId, teacherId, Classroom::setTutor);
    }

    private boolean linkTeacher(
            final UUID classroomId,
            final UUID teacherId,
            final BiConsumer<Classroom, Teacher> link
    ) {
        final Optional<Classroom> classroom = this.classroomRepository.findById(classroomId);
        final Optional<Teacher>   teacher   = this.teacherRepository.findById(teacherId);
        if(classroom.isEmpty() || teacher.isEmpty()) {
            return false;
        }
        this.policy.authorizeUpdate(classroom.get());
        this.verifySchoolMembership(classroom.get().getSchool(), teacher.get());
        link.accept(classroom.get(), teacher.get());
        this.classroomRepository.save(classroom.get());
        return true;
    }

    boolean enrollKid(final UUID classroomId, final UUID kidId) {
        final Optional<Classroom> classroom = this.classroomRepository.findById(classroomId);
        final Optional<Kid>       kid       = this.kidRepository.findById(kidId);
        if(classroom.isEmpty() || kid.isEmpty()) {
            return false;
        }
        this.policy.authorizeUpdate(classroom.get());
        kid.get().setClassroom(classroom.get());
        this.kidRepository.save(kid.get());
        return true;
    }

}
