package edu.lyra.members.api.person.rest;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.exceptions.ParentHasKidsException;
import edu.lyra.members.api.exceptions.TeacherAssignedToClassroomException;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Lets an admin grant or revoke a role — {@code parent}, {@code teacher} — on an existing {@link Person}, so the same
 * human can hold either role, both, or neither, independently of how they first registered. Self-service acquisition
 * still happens through {@code POST /parents} and {@code POST /teachers}; this is for an admin assigning a role to a
 * person after the fact (e.g. a registered parent who also teaches).
 */
@Slf4j
@RequiredArgsConstructor
@RepositoryRestController
class PersonRoleController {

    private final PersonRepository    personRepository;
    private final ParentRepository    parentRepository;
    private final TeacherRepository   teacherRepository;
    private final ClassroomRepository classroomRepository;

    @Qualifier("defaultConversionService")
    private final ConversionService conversionService;

    @PutMapping("/persons/{id}/parent")
    ResponseEntity<Void> grantParentRole(final @PathVariable UUID id) {
        this.requireAdmin();
        return this.personRepository.findById(id).map(person -> {
            if(this.parentRepository.existsById(id)) {
                log.debug("Person {} is already a parent; leaving the parent role unchanged", id);
            } else {
                this.parentRepository.save(Parent.builder().person(person).build());
                log.debug("Granted the parent role to person {}", id);
            }
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> {
            log.debug("Person {} not found; cannot grant the parent role", id);
            return ResponseEntity.notFound().build();
        });
    }

    private void requireAdmin() {
        if(! AuthenticatedPrincipal.hasRole("admin")) {
            log.debug("Authenticated principal is not an admin; refusing role management");
            throw new AccessDeniedException("Only an admin can manage a person's roles");
        }
    }

    @PutMapping("/persons/{id}/teacher")
    ResponseEntity<Void> grantTeacherRole(final @PathVariable UUID id, final @RequestBody Map<String, Object> body) {
        this.requireAdmin();
        final Optional<Person> person = this.personRepository.findById(id);
        if(person.isEmpty()) {
            log.debug("Person {} not found; cannot grant the teacher role", id);
            return ResponseEntity.notFound().build();
        }
        if(this.teacherRepository.existsById(id)) {
            log.debug("Person {} is already a teacher; leaving the teacher role unchanged", id);
            return ResponseEntity.noContent().build();
        }
        final Optional<School> school = this.school(body);
        if(school.isEmpty()) {
            log.debug("No valid school given; cannot grant the teacher role to person {}", id);
            return ResponseEntity.badRequest().build();
        }
        this.teacherRepository.save(Teacher.builder().person(person.get()).school(school.get()).build());
        log.debug("Granted the teacher role to person {} at school {}", id, school.get().getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * The {@code school} value is a HAL URI reference (e.g. {@code /v0/schools/<id>}), matching how every other
     * association in this API — including a teacher's own {@code school} at self-registration — is addressed. Resolved
     * through Spring Data REST's own {@link org.springframework.data.rest.core.UriToEntityConverter}, registered on the
     * {@code defaultConversionService} bean, rather than parsing the URI by hand.
     */
    private Optional<School> school(final Map<String, Object> body) {
        try {
            return Optional.ofNullable(body.get("school"))
                           .map(school -> this.conversionService.convert(URI.create(school.toString()), School.class));
        } catch(final IllegalArgumentException | ConversionException _) {
            return Optional.empty();
        }
    }

    @DeleteMapping("/persons/{id}/parent")
    ResponseEntity<Void> revokeParentRole(final @PathVariable UUID id) {
        this.requireAdmin();
        return this.parentRepository.findById(id).map(parent -> {
            if(! parent.getKids().isEmpty()) {
                log.debug("Parent {} still has kids; refusing to revoke the parent role", id);
                throw new ParentHasKidsException(
                        ("Parent %s still has %d kid(s) linked; remove or reassign them before revoking the parent " +
                         "role").formatted(id, parent.getKids().size()));
            }
            this.parentRepository.delete(parent);
            log.debug("Revoked the parent role from person {}", id);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> {
            log.debug("Person {} does not hold the parent role; nothing to revoke", id);
            return ResponseEntity.notFound().build();
        });
    }

    @DeleteMapping("/persons/{id}/teacher")
    ResponseEntity<Void> revokeTeacherRole(final @PathVariable UUID id) {
        this.requireAdmin();
        return this.teacherRepository.findById(id).map(teacher -> {
            if(this.classroomRepository.existsByTutorIdOrTeachersId(id)) {
                log.debug("Teacher {} is still referenced by a classroom; refusing to revoke the teacher role", id);
                throw new TeacherAssignedToClassroomException(
                        ("Teacher %s still tutors or teaches at least one classroom; unassign them before revoking " +
                         "the teacher role").formatted(id));
            }
            this.teacherRepository.delete(teacher);
            log.debug("Revoked the teacher role from person {}", id);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> {
            log.debug("Person {} does not hold the teacher role; nothing to revoke", id);
            return ResponseEntity.notFound().build();
        });
    }

}
