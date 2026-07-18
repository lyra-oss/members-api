package edu.lyra.members.api.person.rest;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/*
 * A parent's or teacher's identity now lives solely on the shared Person — name, surname and mail on
 * Parent/Teacher are plain bean properties that delegate straight through to it, not JPA-mapped fields —
 * and Spring Data REST's built-in PATCH merge (DomainObjectReader) only merges properties it recognises
 * from the entity's own persistent mapping, so it can't reach a delegating, non-persistent property. This
 * overrides the item-PATCH endpoints for Parent and Teacher to apply the requested changes directly onto
 * the role (which writes each one through to Person on its own), while still publishing the normal
 * before/after-save events for the role entity so authorization (@HandleBeforeSave handlers) and validation
 * keep working exactly as they do for every other resource.
 */
@Slf4j
@RequiredArgsConstructor
@RepositoryRestController
class PersonUpdateController {

    private final ParentRepository  parentRepository;
    private final TeacherRepository teacherRepository;
    private final PersonRepository  personRepository;

    private final ApplicationEventPublisher eventPublisher;

    @PatchMapping("/parents/{id}")
    ResponseEntity<Void> patchParent(final @PathVariable UUID id, final @RequestBody Map<String, Object> body) {
        return this.parentRepository.findById(id).map(parent -> {
            applyPersonFields(body, parent::setName, parent::setSurname, parent::setMail);
            this.saveIdentity(parent, parent.getPerson());
            log.debug("Patched contact info for parent {}", id);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> {
            log.debug("Parent {} not found; nothing to patch", id);
            return ResponseEntity.notFound().build();
        });
    }

    private static void applyPersonFields(
            final Map<String, Object> body,
            final Consumer<String> name,
            final Consumer<String> surname,
            final Consumer<String> mail
    ) {
        if(body.containsKey("name")) {
            name.accept((String) body.get("name"));
        }
        if(body.containsKey("surname")) {
            surname.accept((String) body.get("surname"));
        }
        if(body.containsKey("mail")) {
            mail.accept((String) body.get("mail"));
        }
    }

    private void saveIdentity(final Object role, final Person person) {
        this.eventPublisher.publishEvent(new BeforeSaveEvent(role));
        this.personRepository.save(person);
        this.eventPublisher.publishEvent(new AfterSaveEvent(role));
    }

    @PatchMapping("/teachers/{id}")
    ResponseEntity<Void> patchTeacher(final @PathVariable UUID id, final @RequestBody Map<String, Object> body) {
        return this.teacherRepository.findById(id).map(teacher -> {
            applyPersonFields(body, teacher::setName, teacher::setSurname, teacher::setMail);
            this.saveIdentity(teacher, teacher.getPerson());
            log.debug("Patched contact info for teacher {}", id);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> {
            log.debug("Teacher {} not found; nothing to patch", id);
            return ResponseEntity.notFound().build();
        });
    }

}
