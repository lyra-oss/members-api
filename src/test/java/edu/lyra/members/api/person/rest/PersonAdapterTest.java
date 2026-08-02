package edu.lyra.members.api.person.rest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.exceptions.ParentHasKidsException;
import edu.lyra.members.api.exceptions.TeacherAssignedToClassroomException;
import edu.lyra.members.api.exceptions.UnresolvableReferenceException;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonAdapterTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    private final PersonMapper mapper = Mappers.getMapper(PersonMapper.class);

    private PersonAdapter adapter;

    @BeforeEach
    void setUp() {
        //@formatter:off
        this.adapter = new PersonAdapter(this.personRepository, this.parentRepository, this.teacherRepository,
                                         this.schoolRepository, this.classroomRepository, this.mapper);
        //@formatter:on
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static Person aPerson(final UUID id) {
        //@formatter:off
        return Person.builder().id(id)
                     .name("Esteban")
                     .surname("Cristóbal")
                     .mail("esteban.cristobal@example.com")
                     .build();
        //@formatter:on
    }

    private static School aSchool() {
        final School school = new School();
        school.setName("Gloria Fuertes");
        ReflectionTestUtils.setField(school, "id", UUID.randomUUID());
        return school;
    }

    @Test
    void toModelAddsASelfLink() {
        final Person person = aPerson(UUID.randomUUID());
        final PersonModel model = this.adapter.toModel(person);
        assertEquals("Esteban", model.getName());
        assertTrue(model.getRequiredLink("self").getHref().endsWith("/persons/" + person.getId()));
    }

    @Test
    void findByIdReturnsEmptyWhenThePersonDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.findById(id));
    }

    @Test
    void findByIdReturnsTheModelWhenThePersonExists() {
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.of(aPerson(id)));
        assertEquals("Esteban", this.adapter.findById(id).orElseThrow().getName());
    }

    @Test
    void findAllDelegatesToThePagedResourcesAssembler() {
        final Pageable pageable = PageRequest.of(0, 20);
        final Page<Person> page = new PageImpl<>(List.of(aPerson(UUID.randomUUID())));
        when(this.personRepository.findAll(pageable)).thenReturn(page);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Person> pagedAssembler = mock(PagedResourcesAssembler.class);
        final PagedModel<PersonModel> expected = PagedModel.empty();
        when(pagedAssembler.toModel(page, this.adapter)).thenReturn(expected);
        assertEquals(expected, this.adapter.findAll(pageable, pagedAssembler));
    }

    @Test
    void grantParentRoleReturnsFalseWhenPersonMissing() {
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.empty());
        assertFalse(this.adapter.grantParentRole(id));
    }

    @Test
    void grantParentRoleIsIdempotentWhenAlreadyAParent() {
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.of(aPerson(id)));
        when(this.parentRepository.existsById(id)).thenReturn(true);
        assertTrue(this.adapter.grantParentRole(id));
        verify(this.parentRepository, never()).save(any());
    }

    @Test
    void grantParentRoleSavesANewParentForAnUnclaimedPerson() {
        final UUID   id     = UUID.randomUUID();
        final Person person = aPerson(id);
        when(this.personRepository.findById(id)).thenReturn(Optional.of(person));
        when(this.parentRepository.existsById(id)).thenReturn(false);
        assertTrue(this.adapter.grantParentRole(id));
        final ArgumentCaptor<Parent> captor = ArgumentCaptor.forClass(Parent.class);
        verify(this.parentRepository).save(captor.capture());
        assertEquals(person, captor.getValue().getPerson());
    }

    @Test
    void grantTeacherRoleReturnsFalseWhenPersonMissing() {
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.empty());
        assertFalse(this.adapter.grantTeacherRole(id, new GrantTeacherRoleRequest(UUID.randomUUID())));
    }

    @Test
    void grantTeacherRoleIsIdempotentWhenAlreadyATeacher() {
        final UUID id = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.of(aPerson(id)));
        when(this.teacherRepository.existsById(id)).thenReturn(true);
        assertTrue(this.adapter.grantTeacherRole(id, new GrantTeacherRoleRequest(UUID.randomUUID())));
        verify(this.teacherRepository, never()).save(any());
    }

    @Test
    void grantTeacherRoleFailsWithAnUnresolvableReferenceWhenTheSchoolDoesNotExist() {
        final UUID id            = UUID.randomUUID();
        final UUID unknownSchool = UUID.randomUUID();
        when(this.personRepository.findById(id)).thenReturn(Optional.of(aPerson(id)));
        when(this.teacherRepository.existsById(id)).thenReturn(false);
        when(this.schoolRepository.findById(unknownSchool)).thenReturn(Optional.empty());
        final GrantTeacherRoleRequest request = new GrantTeacherRoleRequest(unknownSchool);
        assertThrows(UnresolvableReferenceException.class, () -> this.adapter.grantTeacherRole(id, request));
        verify(this.teacherRepository, never()).save(any());
    }

    @Test
    void grantTeacherRoleSavesANewTeacherWithTheResolvedSchool() {
        final UUID   id     = UUID.randomUUID();
        final Person person = aPerson(id);
        final School school = aSchool();
        when(this.personRepository.findById(id)).thenReturn(Optional.of(person));
        when(this.teacherRepository.existsById(id)).thenReturn(false);
        when(this.schoolRepository.findById(school.getId())).thenReturn(Optional.of(school));
        assertTrue(this.adapter.grantTeacherRole(id, new GrantTeacherRoleRequest(school.getId())));
        final ArgumentCaptor<Teacher> captor = ArgumentCaptor.forClass(Teacher.class);
        verify(this.teacherRepository).save(captor.capture());
        assertEquals(school, captor.getValue().getSchool());
        assertEquals(person, captor.getValue().getPerson());
    }

    @Test
    void revokeParentRoleReturnsFalseWhenNotAParent() {
        final UUID id = UUID.randomUUID();
        when(this.parentRepository.findById(id)).thenReturn(Optional.empty());
        assertFalse(this.adapter.revokeParentRole(id));
    }

    @Test
    void revokeParentRoleRejectsWhenParentStillHasKids() {
        final UUID   id     = UUID.randomUUID();
        final Parent parent = mock(Parent.class);
        when(parent.getKids()).thenReturn(Set.of(mock(Kid.class)));
        when(this.parentRepository.findById(id)).thenReturn(Optional.of(parent));
        assertThrows(ParentHasKidsException.class, () -> this.adapter.revokeParentRole(id));
        verify(this.parentRepository, never()).delete(any());
    }

    @Test
    void revokeParentRoleDeletesTheParentWhenChildless() {
        final UUID   id     = UUID.randomUUID();
        final Parent parent = mock(Parent.class);
        when(parent.getKids()).thenReturn(Set.of());
        when(this.parentRepository.findById(id)).thenReturn(Optional.of(parent));
        assertTrue(this.adapter.revokeParentRole(id));
        verify(this.parentRepository).delete(parent);
    }

    @Test
    void revokeTeacherRoleReturnsFalseWhenNotATeacher() {
        final UUID id = UUID.randomUUID();
        when(this.teacherRepository.findById(id)).thenReturn(Optional.empty());
        assertFalse(this.adapter.revokeTeacherRole(id));
    }

    @Test
    void revokeTeacherRoleRejectsWhenReferencedByAClassroom() {
        final UUID    id      = UUID.randomUUID();
        final Teacher teacher = mock(Teacher.class);
        when(this.teacherRepository.findById(id)).thenReturn(Optional.of(teacher));
        when(this.classroomRepository.existsByTutorIdOrTeachersId(id)).thenReturn(true);
        assertThrows(TeacherAssignedToClassroomException.class, () -> this.adapter.revokeTeacherRole(id));
        verify(this.teacherRepository, never()).delete(any());
    }

    @Test
    void revokeTeacherRoleDeletesTheTeacherWhenUnreferenced() {
        final UUID    id      = UUID.randomUUID();
        final Teacher teacher = mock(Teacher.class);
        when(this.teacherRepository.findById(id)).thenReturn(Optional.of(teacher));
        when(this.classroomRepository.existsByTutorIdOrTeachersId(id)).thenReturn(false);
        assertTrue(this.adapter.revokeTeacherRole(id));
        verify(this.teacherRepository).delete(teacher);
    }

}
