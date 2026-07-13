package edu.lyra.members.api.mvc;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.lyra.members.api.handlers.TeacherSchoolMembership;
import edu.lyra.members.api.repositories.jpa.Classroom;
import edu.lyra.members.api.repositories.jpa.ClassroomsRepository;
import edu.lyra.members.api.repositories.jpa.Teacher;
import edu.lyra.members.api.repositories.jpa.TeachersRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RepositoryRestController
class ClassroomTeachingStaffController {

    private final ClassroomsRepository classroomsRepository;
    private final TeachersRepository teachersRepository;

    ClassroomTeachingStaffController(
            final ClassroomsRepository classroomsRepository,
            final TeachersRepository teachersRepository
    ) {
        this.classroomsRepository = classroomsRepository;
        this.teachersRepository = teachersRepository;
    }

    @PostMapping(path = "/classrooms/{id}/teachers", consumes = "text/uri-list")
    ResponseEntity<Void> addTeacher(@PathVariable final UUID id, final HttpServletRequest request)
            throws IOException {
        final Classroom classroom = this.findClassroom(id);
        final Teacher teacher = this.resolveTeacher(readBody(request));
        TeacherSchoolMembership.verifyBelongsToSchool(classroom.getSchool(), teacher);
        classroom.getTeachers().add(teacher);
        this.classroomsRepository.save(classroom);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(path = "/classrooms/{id}/tutor", consumes = "text/uri-list")
    ResponseEntity<Void> setTutor(@PathVariable final UUID id, final HttpServletRequest request)
            throws IOException {
        final Classroom classroom = this.findClassroom(id);
        final Teacher teacher = this.resolveTeacher(readBody(request));
        TeacherSchoolMembership.verifyBelongsToSchool(classroom.getSchool(), teacher);
        classroom.setTutor(teacher);
        this.classroomsRepository.save(classroom);
        return ResponseEntity.noContent().build();
    }

    private static String readBody(final HttpServletRequest request)
            throws IOException {
        return request.getReader().lines().collect(Collectors.joining("\n"));
    }

    private Classroom findClassroom(final UUID id) {
        return this.classroomsRepository.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    private Teacher resolveTeacher(final String body) {
        final String uri = body.strip().lines().findFirst()
                               .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Missing teacher URI"));
        final UUID teacherId = UUID.fromString(uri.substring(uri.lastIndexOf('/') + 1));
        return this.teachersRepository.findById(teacherId)
                                      .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Teacher not found"));
    }

}
