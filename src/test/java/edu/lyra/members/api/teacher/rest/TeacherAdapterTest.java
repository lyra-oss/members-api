package edu.lyra.members.api.teacher.rest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.exceptions.UnresolvableReferenceException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherAdapterTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    private final TeacherMapper mapper = Mappers.getMapper(TeacherMapper.class);

    private TeacherPolicy policy;

    private TeacherAdapter adapter;

    @BeforeEach
    void setUp() {
        this.policy = mock(TeacherPolicy.class);
        //@formatter:off
        this.adapter = new TeacherAdapter(this.teacherRepository, this.schoolRepository, this.personRepository,
                                          this.classroomRepository, this.mapper, this.policy);
        //@formatter:on
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(final UUID id) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final Authentication authentication = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Teacher aTeacher(final String name) {
        final Teacher teacher = new Teacher();
        teacher.setName(name);
        teacher.setSurname("Ibáñez");
        teacher.setMail("marta.ibanez@example.com");
        ReflectionTestUtils.setField(teacher, "id", UUID.randomUUID());
        return teacher;
    }

    private static School aSchool() {
        final School school = new School();
        school.setName("Gloria Fuertes");
        ReflectionTestUtils.setField(school, "id", UUID.randomUUID());
        return school;
    }

    @Test
    void toModelAddsASelfLink() {
        final Teacher teacher = aTeacher("Marta");
        final TeacherModel model = this.adapter.toModel(teacher);
        assertEquals("Marta", model.getName());
        assertTrue(model.getRequiredLink("self").getHref().endsWith("/teachers/" + teacher.getId()));
    }

    @Test
    void findByIdReturnsEmptyWhenTheTeacherDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.teacherRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.findById(id));
    }

    @Test
    void findByIdReturnsTheModelWhenTheTeacherExists() {
        final UUID    id      = UUID.randomUUID();
        final Teacher teacher = aTeacher("Marta");
        when(this.teacherRepository.findById(id)).thenReturn(Optional.of(teacher));
        assertEquals("Marta", this.adapter.findById(id).orElseThrow().getName());
    }

    @Test
    void findAllDelegatesToThePagedResourcesAssembler() {
        final Pageable pageable = PageRequest.of(0, 20);
        final Page<Teacher> page = new PageImpl<>(List.of(aTeacher("Marta")));
        when(this.teacherRepository.findAll(pageable)).thenReturn(page);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Teacher> pagedAssembler = mock(PagedResourcesAssembler.class);
        final PagedModel<TeacherModel> expected = PagedModel.empty();
        when(pagedAssembler.toModel(page, this.adapter)).thenReturn(expected);
        assertEquals(expected, this.adapter.findAll(pageable, pagedAssembler));
    }

    @Test
    void createFailsWithAnUnresolvableReferenceWhenTheSchoolDoesNotExist() {
        final UUID unknownSchool = UUID.randomUUID();
        when(this.schoolRepository.findById(unknownSchool)).thenReturn(Optional.empty());
        final TeacherRequest request = new TeacherRequest("Marta", "Ibáñez", "marta.ibanez@example.com",
                                                          unknownSchool);
        assertThrows(UnresolvableReferenceException.class, () -> this.adapter.create(request));
    }

    @Test
    void createUsesTheExistingPersonWhenTheAuthenticatedSubjectIsAlreadyRegistered() {
        final School school  = aSchool();
        final UUID   subject = UUID.randomUUID();
        authenticateAs(subject);
        when(this.schoolRepository.findById(school.getId())).thenReturn(Optional.of(school));
        final Person existingPerson = Person.builder().id(subject).name("Already").surname("Registered")
                                            .mail("already.registered@example.com").build();
        when(this.personRepository.findById(subject)).thenReturn(Optional.of(existingPerson));
        when(this.teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));
        final TeacherRequest request = new TeacherRequest("Marta", "Ibáñez", "marta.ibanez@example.com",
                                                          school.getId());
        final TeacherModel model = this.adapter.create(request);
        // the existing person's identity wins over whatever the request supplied
        assertEquals("Already", model.getName());
        assertEquals("Registered", model.getSurname());
    }

    @Test
    void createBuildsANewPersonUnderTheSubjectWhenNonePreviouslyExisted() {
        final School school  = aSchool();
        final UUID   subject = UUID.randomUUID();
        authenticateAs(subject);
        when(this.schoolRepository.findById(school.getId())).thenReturn(Optional.of(school));
        when(this.personRepository.findById(subject)).thenReturn(Optional.empty());
        when(this.teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));
        final TeacherRequest request = new TeacherRequest("Marta", "Ibáñez", "marta.ibanez@example.com",
                                                          school.getId());
        final TeacherModel model = this.adapter.create(request);
        // the request's own identity is used since no person was previously registered under the
        // subject; @MapsId then derives the teacher's own id from person.id, but only Hibernate does
        // that at real persist time, so it is out of scope for this mocked-repository test.
        assertEquals("Marta", model.getName());
        final ArgumentCaptor<Teacher> saved = ArgumentCaptor.forClass(Teacher.class);
        verify(this.teacherRepository).save(saved.capture());
        assertEquals(subject, saved.getValue().getPerson().getId());
    }

    @Test
    void updateReturnsEmptyWhenTheTeacherDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.teacherRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.update(id, new TeacherPatchRequest(null, "New surname", null)));
    }

    @Test
    void updateAuthorizesBeforeSaving() {
        final UUID    id      = UUID.randomUUID();
        final Teacher teacher = aTeacher("Marta");
        when(this.teacherRepository.findById(id)).thenReturn(Optional.of(teacher));
        when(this.teacherRepository.save(teacher)).thenReturn(teacher);
        final TeacherModel model =
                this.adapter.update(id, new TeacherPatchRequest(null, "New surname", null)).orElseThrow();
        verify(this.policy).authorizeUpdate(teacher);
        assertEquals("New surname", model.getSurname());
    }

    @Test
    void updatePropagatesAnUnauthorizedRejectionWithoutSaving() {
        final UUID    id      = UUID.randomUUID();
        final Teacher teacher = aTeacher("Marta");
        when(this.teacherRepository.findById(id)).thenReturn(Optional.of(teacher));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeUpdate(teacher);
        final TeacherPatchRequest request = new TeacherPatchRequest(null, "New surname", null);
        assertThrows(AccessDeniedException.class, () -> this.adapter.update(id, request));
        verify(this.teacherRepository, never()).save(any());
    }

    @Test
    void deleteReturnsFalseWhenTheTeacherDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.teacherRepository.findById(id)).thenReturn(Optional.empty());
        assertFalse(this.adapter.delete(id));
    }

    @Test
    void deleteAuthorizesBeforeDeleting() {
        final UUID    id      = UUID.randomUUID();
        final Teacher teacher = aTeacher("Marta");
        when(this.teacherRepository.findById(id)).thenReturn(Optional.of(teacher));
        assertTrue(this.adapter.delete(id));
        verify(this.policy).authorizeDelete(teacher);
        verify(this.teacherRepository).delete(teacher);
    }

    @Test
    void deletePropagatesAnUnauthorizedRejectionWithoutDeleting() {
        final UUID    id      = UUID.randomUUID();
        final Teacher teacher = aTeacher("Marta");
        when(this.teacherRepository.findById(id)).thenReturn(Optional.of(teacher));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeDelete(teacher);
        assertThrows(AccessDeniedException.class, () -> this.adapter.delete(id));
        verify(this.teacherRepository, never()).delete(any());
    }

    @Test
    void findBySchoolReturnsEmptyWhenTheSchoolDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.schoolRepository.existsById(id)).thenReturn(false);
        final Pageable pageable = PageRequest.of(0, 20);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Teacher> pagedAssembler = mock(PagedResourcesAssembler.class);
        assertEquals(Optional.empty(), this.adapter.findBySchool(id, pageable, pagedAssembler));
    }

    @Test
    void findBySchoolReturnsThePagedTeachers() {
        final UUID id = UUID.randomUUID();
        when(this.schoolRepository.existsById(id)).thenReturn(true);
        final Pageable pageable = PageRequest.of(0, 20);
        final Page<Teacher> page = new PageImpl<>(List.of(aTeacher("Marta")));
        when(this.teacherRepository.findBySchoolId(id, pageable)).thenReturn(page);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Teacher> pagedAssembler = mock(PagedResourcesAssembler.class);
        final PagedModel<TeacherModel> expected = PagedModel.empty();
        when(pagedAssembler.toModel(page, this.adapter)).thenReturn(expected);
        assertEquals(expected, this.adapter.findBySchool(id, pageable, pagedAssembler).orElseThrow());
    }

    @Test
    void findByClassroomReturnsEmptyWhenTheClassroomDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.classroomRepository.existsById(id)).thenReturn(false);
        final Pageable pageable = PageRequest.of(0, 20);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Teacher> pagedAssembler = mock(PagedResourcesAssembler.class);
        assertEquals(Optional.empty(), this.adapter.findByClassroom(id, pageable, pagedAssembler));
    }

    @Test
    void findByClassroomReturnsThePagedTeachers() {
        final UUID id = UUID.randomUUID();
        when(this.classroomRepository.existsById(id)).thenReturn(true);
        final Pageable pageable = PageRequest.of(0, 20);
        final Page<Teacher> page = new PageImpl<>(List.of(aTeacher("Marta")));
        when(this.teacherRepository.findByClassroomId(id, pageable)).thenReturn(page);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Teacher> pagedAssembler = mock(PagedResourcesAssembler.class);
        final PagedModel<TeacherModel> expected = PagedModel.empty();
        when(pagedAssembler.toModel(page, this.adapter)).thenReturn(expected);
        assertEquals(expected, this.adapter.findByClassroom(id, pageable, pagedAssembler).orElseThrow());
    }

    @Test
    void findTutorOfReturnsEmptyWhenTheClassroomDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.classroomRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.findTutorOf(id));
    }

    @Test
    void findTutorOfReturnsEmptyWhenNoTutorIsSet() {
        final UUID      id        = UUID.randomUUID();
        final Classroom classroom = new Classroom();
        when(this.classroomRepository.findById(id)).thenReturn(Optional.of(classroom));
        assertEquals(Optional.empty(), this.adapter.findTutorOf(id));
    }

    @Test
    void findTutorOfReturnsTheTutor() {
        final UUID      id        = UUID.randomUUID();
        final Teacher   tutor     = aTeacher("Marta");
        final Classroom classroom = new Classroom();
        classroom.setTutor(tutor);
        when(this.classroomRepository.findById(id)).thenReturn(Optional.of(classroom));
        assertEquals("Marta", this.adapter.findTutorOf(id).orElseThrow().getName());
    }

}
