package edu.lyra.members.api.contactinfo.rest;

import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

import edu.lyra.members.api.contactinfo.ContactInfo;
import edu.lyra.members.api.parent.ParentsRepository;
import edu.lyra.members.api.teacher.TeachersRepository;
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

/**
 * Spring Data REST's built-in PATCH merge (DomainObjectReader) does not correctly apply changes onto an
 * {@code @JsonUnwrapped @Embedded} value object such as {@link ContactInfo} — it silently drops every change to the
 * unwrapped fields, regardless of what the request body contains. This overrides the item-PATCH endpoints for
 * {@code Parent} and {@code Teacher} (the only two entities with such a property) to apply the merge manually, while
 * still going through the normal before/after-save event publication so authorization ({@code @HandleBeforeSave}
 * handlers) and validation keep working exactly as they do for every other resource.
 */
@Slf4j
@RequiredArgsConstructor
@RepositoryRestController
class ContactInfoUpdateController {

    private final ParentsRepository         parentsRepository;
    private final TeachersRepository        teachersRepository;
    private final ApplicationEventPublisher eventPublisher;

    @PatchMapping("/parents/{id}")
    ResponseEntity<Void> patchParent(final @PathVariable UUID id, final @RequestBody Map<String, Object> body) {
        return this.parentsRepository.findById(id).map(parent -> {
            applyContactInfo(parent.getContactInfo(), body);
            this.save(this.parentsRepository::save, parent);
            log.debug("Patched contact info for parent {}", id);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> {
            log.debug("Parent {} not found; nothing to patch", id);
            return ResponseEntity.notFound().build();
        });
    }

    private static void applyContactInfo(final ContactInfo contactInfo, final Map<String, Object> body) {
        if(body.containsKey("name")) {
            contactInfo.setName((String) body.get("name"));
        }
        if(body.containsKey("surname")) {
            contactInfo.setSurname((String) body.get("surname"));
        }
        if(body.containsKey("mail")) {
            contactInfo.setMail((String) body.get("mail"));
        }
    }

    private <T> void save(final UnaryOperator<T> save, final T entity) {
        this.eventPublisher.publishEvent(new BeforeSaveEvent(entity));
        save.apply(entity);
        this.eventPublisher.publishEvent(new AfterSaveEvent(entity));
    }

    @PatchMapping("/teachers/{id}")
    ResponseEntity<Void> patchTeacher(final @PathVariable UUID id, final @RequestBody Map<String, Object> body) {
        return this.teachersRepository.findById(id).map(teacher -> {
            applyContactInfo(teacher.getContactInfo(), body);
            this.save(this.teachersRepository::save, teacher);
            log.debug("Patched contact info for teacher {}", id);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> {
            log.debug("Teacher {} not found; nothing to patch", id);
            return ResponseEntity.notFound().build();
        });
    }

}
