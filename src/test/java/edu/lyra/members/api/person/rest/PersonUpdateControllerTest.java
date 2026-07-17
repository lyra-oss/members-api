package edu.lyra.members.api.person.rest;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonUpdateControllerTest {

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PersonUpdateController controller;

    @BeforeEach
    void setUp() {
        this.controller =
                new PersonUpdateController(this.parentRepository, this.teacherRepository, this.personRepository,
                                           this.eventPublisher);
    }

    @Test
    void patchParentAppliesEveryProvidedFieldAndPublishesSaveEvents() {
        final UUID   id     = UUID.randomUUID();
        final Person person = aPerson(id);
        final Parent parent = Parent.builder().person(person).build();
        when(this.parentRepository.findById(id)).thenReturn(Optional.of(parent));
        //@formatter:off
        final var response = this.controller.patchParent(id,
                Map.of("name", "New",
                       "surname", "NewSurname",
                       "mail", "new@example.com"));
        //@formatter:on
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals("New", person.getName());
        assertEquals("NewSurname", person.getSurname());
        assertEquals("new@example.com", person.getMail());
        assertEquals("New", parent.getName());
        assertEquals("NewSurname", parent.getSurname());
        assertEquals("new@example.com", parent.getMail());
        verify(this.personRepository).save(person);
        verify(this.eventPublisher).publishEvent(any(BeforeSaveEvent.class));
        verify(this.eventPublisher).publishEvent(any(AfterSaveEvent.class));
    }

    private static Person aPerson(final UUID id) {
        //@formatter:off
        return Person.builder().id(id)
                               .name("Old")
                               .surname("OldSurname")
                               .mail("old@example.com")
                     .build();
        //@formatter:on
    }

    @Test
    void patchParentLeavesFieldsNotPresentInTheBodyUnchanged() {
        final UUID   id     = UUID.randomUUID();
        final Person person = aPerson(id);
        final Parent parent = Parent.builder().person(person).build();
        when(this.parentRepository.findById(id)).thenReturn(Optional.of(parent));
        this.controller.patchParent(id, Map.of("surname", "Cristóbal"));
        assertEquals("Old", person.getName());
        assertEquals("Cristóbal", person.getSurname());
        assertEquals("old@example.com", person.getMail());
    }

    @Test
    void patchParentReturnsNotFoundWhenParentDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.parentRepository.findById(id)).thenReturn(Optional.empty());
        final var response = this.controller.patchParent(id, Map.of("surname", "Whatever"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verifyNoInteractions(this.eventPublisher);
    }

    @Test
    void patchTeacherAppliesEveryProvidedFieldAndPublishesSaveEvents() {
        final UUID    id      = UUID.randomUUID();
        final Person  person  = aPerson(id);
        final Teacher teacher = Teacher.builder().person(person).build();
        when(this.teacherRepository.findById(id)).thenReturn(Optional.of(teacher));
        //@formatter:off
        final var response = this.controller.patchTeacher(id,
                Map.of("name", "New",
                       "surname", "NewSurname",
                       "mail", "new@example.com"));
        //@formatter:on
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertEquals("New", person.getName());
        assertEquals("NewSurname", person.getSurname());
        assertEquals("new@example.com", person.getMail());
        assertEquals("New", teacher.getName());
        assertEquals("NewSurname", teacher.getSurname());
        assertEquals("new@example.com", teacher.getMail());
        verify(this.personRepository).save(person);
        verify(this.eventPublisher).publishEvent(any(BeforeSaveEvent.class));
        verify(this.eventPublisher).publishEvent(any(AfterSaveEvent.class));
    }

    @Test
    void patchTeacherReturnsNotFoundWhenTeacherDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.teacherRepository.findById(id)).thenReturn(Optional.empty());
        final var response = this.controller.patchTeacher(id, Map.of("surname", "Whatever"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verifyNoInteractions(this.eventPublisher);
    }

}
