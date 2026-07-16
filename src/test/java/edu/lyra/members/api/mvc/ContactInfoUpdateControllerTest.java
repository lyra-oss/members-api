package edu.lyra.members.api.mvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.ContactInfo;
import edu.lyra.members.api.repositories.jpa.Parent;
import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import edu.lyra.members.api.repositories.jpa.Teacher;
import edu.lyra.members.api.repositories.jpa.TeachersRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContactInfoUpdateControllerTest {

    private final ParentsRepository         parentsRepository  = mock(ParentsRepository.class);
    private final TeachersRepository        teachersRepository = mock(TeachersRepository.class);
    private final ApplicationEventPublisher eventPublisher     = mock(ApplicationEventPublisher.class);

    private final ContactInfoUpdateController controller =
            new ContactInfoUpdateController(this.parentsRepository, this.teachersRepository, this.eventPublisher);

    @Test
    void patchParentAppliesEveryProvidedFieldAndPublishesSaveEvents() {
        final UUID   id     = UUID.randomUUID();
        final Parent parent = aParentWithContactInfo(id, "Old", "OldSurname", "old@example.com");
        when(this.parentsRepository.findById(id)).thenReturn(Optional.of(parent));
        //@formatter:off
        final ResponseEntity<Void> response = this.controller.patchParent(id,
                Map.of("name", "New",
                       "surname", "NewSurname",
                       "mail", "new@example.com"));
        //@formatter:on
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(parent.getContactInfo().getName()).isEqualTo("New");
        assertThat(parent.getContactInfo().getSurname()).isEqualTo("NewSurname");
        assertThat(parent.getContactInfo().getMail()).isEqualTo("new@example.com");
        verify(this.parentsRepository).save(parent);
        verify(this.eventPublisher).publishEvent(any(BeforeSaveEvent.class));
        verify(this.eventPublisher).publishEvent(any(AfterSaveEvent.class));
    }

    private static Parent aParentWithContactInfo(
            final UUID id,
            final String name,
            final String surname,
            final String mail
    ) {
        return Parent.builder().id(id).contactInfo(ContactInfo.builder().name(name).surname(surname).mail(mail).build())
                     .build();
    }

    @Test
    void patchParentLeavesFieldsNotPresentInTheBodyUnchanged() {
        final UUID   id     = UUID.randomUUID();
        final Parent parent = aParentWithContactInfo(id, "Esteban", "Cristóbal", "esteban@example.com");
        when(this.parentsRepository.findById(id)).thenReturn(Optional.of(parent));
        this.controller.patchParent(id, Map.of("surname", "Cristóbal Ruiz"));
        assertThat(parent.getContactInfo().getName()).isEqualTo("Esteban");
        assertThat(parent.getContactInfo().getSurname()).isEqualTo("Cristóbal Ruiz");
        assertThat(parent.getContactInfo().getMail()).isEqualTo("esteban@example.com");
    }

    @Test
    void patchParentReturnsNotFoundWhenParentDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.parentsRepository.findById(id)).thenReturn(Optional.empty());
        final ResponseEntity<Void> response = this.controller.patchParent(id, Map.of("surname", "Whatever"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(this.eventPublisher);
    }

    @Test
    void patchTeacherAppliesEveryProvidedFieldAndPublishesSaveEvents() {
        final UUID    id      = UUID.randomUUID();
        final Teacher teacher = aTeacherWithContactInfo(id);
        when(this.teachersRepository.findById(id)).thenReturn(Optional.of(teacher));
        //@formatter:off
        final ResponseEntity<Void> response = this.controller.patchTeacher(id,
                Map.of("name", "New",
                       "surname", "NewSurname",
                       "mail", "new@example.com"));
        //@formatter:on
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(teacher.getContactInfo().getName()).isEqualTo("New");
        assertThat(teacher.getContactInfo().getSurname()).isEqualTo("NewSurname");
        assertThat(teacher.getContactInfo().getMail()).isEqualTo("new@example.com");
        verify(this.teachersRepository).save(teacher);
        verify(this.eventPublisher).publishEvent(any(BeforeSaveEvent.class));
        verify(this.eventPublisher).publishEvent(any(AfterSaveEvent.class));
    }

    private static Teacher aTeacherWithContactInfo(
            final UUID id
    ) {
        return Teacher.builder().id(id).contactInfo(
                ContactInfo.builder().name("Old").surname("OldSurname").mail("old@example.com").build()).build();
    }

    @Test
    void patchTeacherReturnsNotFoundWhenTeacherDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.teachersRepository.findById(id)).thenReturn(Optional.empty());
        final ResponseEntity<Void> response = this.controller.patchTeacher(id, Map.of("surname", "Whatever"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(this.eventPublisher);
    }

}
