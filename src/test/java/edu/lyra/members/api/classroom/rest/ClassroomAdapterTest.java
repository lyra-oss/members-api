package edu.lyra.members.api.classroom.rest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.web.ApiBasePath;
import edu.lyra.members.api.exceptions.SchoolMismatchException;
import edu.lyra.members.api.exceptions.UnresolvableReferenceException;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomAdapterTest {

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private KidRepository kidRepository;

    private final ClassroomMapper mapper = Mappers.getMapper(ClassroomMapper.class);

    private ClassroomPolicy policy;

    private ClassroomAdapter adapter;

    @BeforeEach
    void setUp() {
        this.policy = mock(ClassroomPolicy.class);
        //@formatter:off
        this.adapter = new ClassroomAdapter(this.classroomRepository, this.schoolRepository, this.teacherRepository,
                                            this.kidRepository, this.mapper, this.policy, new ApiBasePath("/v0"));
        //@formatter:on
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    private static School aSchool() {
        final School school = new School();
        school.setName("Gloria Fuertes");
        ReflectionTestUtils.setField(school, "id", UUID.randomUUID());
        return school;
    }

    private static Teacher aTeacherAt(final School school) {
        final Teacher teacher = new Teacher();
        teacher.setSchool(school);
        ReflectionTestUtils.setField(teacher, "id", UUID.randomUUID());
        return teacher;
    }

    private static Classroom aClassroom(final School school) {
        final Classroom classroom = new Classroom();
        classroom.setCourse(3);
        classroom.setGroup("A");
        classroom.setSchool(school);
        ReflectionTestUtils.setField(classroom, "id", UUID.randomUUID());
        return classroom;
    }

    @Test
    void toModelAddsASelfLinkPrefixedWithTheApiBasePath() {
        final Classroom classroom = aClassroom(aSchool());
        final ClassroomModel model = this.adapter.toModel(classroom);
        assertEquals(3, model.getCourse());
        assertTrue(model.getRequiredLink("self").getHref().endsWith("/v0/classrooms/" + classroom.getId()));
    }

    @Test
    void findByIdReturnsEmptyWhenTheClassroomDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.classroomRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.findById(id));
    }

    @Test
    void findByIdReturnsTheModelWhenTheClassroomExists() {
        final UUID      id        = UUID.randomUUID();
        final Classroom classroom = aClassroom(aSchool());
        when(this.classroomRepository.findById(id)).thenReturn(Optional.of(classroom));
        assertEquals(3, this.adapter.findById(id).orElseThrow().getCourse());
    }

    @Test
    void findAllDelegatesToThePagedResourcesAssembler() {
        final Pageable pageable = PageRequest.of(0, 20);
        final Page<Classroom> page = new PageImpl<>(List.of(aClassroom(aSchool())));
        when(this.classroomRepository.findAll(pageable)).thenReturn(page);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Classroom> pagedAssembler = mock(PagedResourcesAssembler.class);
        final PagedModel<ClassroomModel> expected = PagedModel.empty();
        when(pagedAssembler.toModel(page, this.adapter)).thenReturn(expected);
        assertEquals(expected, this.adapter.findAll(pageable, pagedAssembler));
    }

    @Test
    void createFailsWithAnUnresolvableReferenceWhenTheSchoolDoesNotExist() {
        final UUID unknownSchool = UUID.randomUUID();
        when(this.schoolRepository.findById(unknownSchool)).thenReturn(Optional.empty());
        final ClassroomRequest request = new ClassroomRequest(3, "A", unknownSchool, null);
        assertThrows(UnresolvableReferenceException.class, () -> this.adapter.create(request));
    }

    @Test
    void createFailsWithAnUnresolvableReferenceWhenTheTutorDoesNotExist() {
        final School school       = aSchool();
        final UUID   unknownTutor = UUID.randomUUID();
        when(this.schoolRepository.findById(school.getId())).thenReturn(Optional.of(school));
        when(this.teacherRepository.findById(unknownTutor)).thenReturn(Optional.empty());
        final ClassroomRequest request = new ClassroomRequest(3, "A", school.getId(), unknownTutor);
        assertThrows(UnresolvableReferenceException.class, () -> this.adapter.create(request));
    }

    @Test
    void createSucceedsWithoutATutor() {
        final School school = aSchool();
        when(this.schoolRepository.findById(school.getId())).thenReturn(Optional.of(school));
        when(this.classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> inv.getArgument(0));
        final ClassroomModel model = this.adapter.create(new ClassroomRequest(3, "A", school.getId(), null));
        assertEquals(3, model.getCourse());
        assertEquals("A", model.getGroup());
    }

    @Test
    void createFailsWhenTheTutorBelongsToADifferentSchool() {
        final School  school = aSchool();
        final Teacher tutor  = aTeacherAt(aSchool());
        when(this.schoolRepository.findById(school.getId())).thenReturn(Optional.of(school));
        when(this.teacherRepository.findById(tutor.getId())).thenReturn(Optional.of(tutor));
        final ClassroomRequest request = new ClassroomRequest(3, "A", school.getId(), tutor.getId());
        assertThrows(SchoolMismatchException.class, () -> this.adapter.create(request));
        verify(this.classroomRepository, never()).save(any());
    }

    @Test
    void createSucceedsWhenTheTutorBelongsToTheSameSchool() {
        final School  school = aSchool();
        final Teacher tutor  = aTeacherAt(school);
        when(this.schoolRepository.findById(school.getId())).thenReturn(Optional.of(school));
        when(this.teacherRepository.findById(tutor.getId())).thenReturn(Optional.of(tutor));
        when(this.classroomRepository.save(any(Classroom.class))).thenAnswer(inv -> inv.getArgument(0));
        final ClassroomModel model = this.adapter.create(new ClassroomRequest(3, "A", school.getId(), tutor.getId()));
        assertEquals(3, model.getCourse());
    }

    @Test
    void updateReturnsEmptyWhenTheClassroomDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.classroomRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.update(id, new ClassroomPatchRequest(4, "B")));
    }

    @Test
    void updateAuthorizesBeforeSaving() {
        final UUID      id        = UUID.randomUUID();
        final Classroom classroom = aClassroom(aSchool());
        when(this.classroomRepository.findById(id)).thenReturn(Optional.of(classroom));
        when(this.classroomRepository.save(classroom)).thenReturn(classroom);
        final ClassroomModel model = this.adapter.update(id, new ClassroomPatchRequest(4, "B")).orElseThrow();
        verify(this.policy).authorizeUpdate(classroom);
        assertEquals(4, model.getCourse());
        assertEquals("B", model.getGroup());
    }

    @Test
    void updatePropagatesAnUnauthorizedRejectionWithoutSaving() {
        final UUID      id        = UUID.randomUUID();
        final Classroom classroom = aClassroom(aSchool());
        when(this.classroomRepository.findById(id)).thenReturn(Optional.of(classroom));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeUpdate(classroom);
        final ClassroomPatchRequest request = new ClassroomPatchRequest(4, "B");
        assertThrows(AccessDeniedException.class, () -> this.adapter.update(id, request));
        verify(this.classroomRepository, never()).save(any());
    }

    @Test
    void deleteReturnsFalseWhenTheClassroomDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.classroomRepository.findById(id)).thenReturn(Optional.empty());
        assertFalse(this.adapter.delete(id));
    }

    @Test
    void deleteAuthorizesBeforeDeleting() {
        final UUID      id        = UUID.randomUUID();
        final Classroom classroom = aClassroom(aSchool());
        when(this.classroomRepository.findById(id)).thenReturn(Optional.of(classroom));
        assertTrue(this.adapter.delete(id));
        verify(this.policy).authorizeDelete(classroom);
        verify(this.classroomRepository).delete(classroom);
    }

    @Test
    void deletePropagatesAnUnauthorizedRejectionWithoutDeleting() {
        final UUID      id        = UUID.randomUUID();
        final Classroom classroom = aClassroom(aSchool());
        when(this.classroomRepository.findById(id)).thenReturn(Optional.of(classroom));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeDelete(classroom);
        assertThrows(AccessDeniedException.class, () -> this.adapter.delete(id));
        verify(this.classroomRepository, never()).delete(any());
    }

    @Test
    void addTeacherReturnsFalseWhenTheClassroomDoesNotExist() {
        final UUID    classroomId = UUID.randomUUID();
        final Teacher teacher     = aTeacherAt(aSchool());
        when(this.classroomRepository.findById(classroomId)).thenReturn(Optional.empty());
        when(this.teacherRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        assertFalse(this.adapter.addTeacher(classroomId, teacher.getId()));
        verify(this.classroomRepository, never()).save(any());
    }

    @Test
    void addTeacherReturnsFalseWhenTheTeacherDoesNotExist() {
        final Classroom classroom = aClassroom(aSchool());
        final UUID       teacherId = UUID.randomUUID();
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.teacherRepository.findById(teacherId)).thenReturn(Optional.empty());
        assertFalse(this.adapter.addTeacher(classroom.getId(), teacherId));
        verify(this.classroomRepository, never()).save(any());
    }

    @Test
    void addTeacherFailsWhenTheTeacherBelongsToADifferentSchool() {
        final School    school    = aSchool();
        final Classroom classroom = aClassroom(school);
        final Teacher   teacher   = aTeacherAt(aSchool());
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.teacherRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        final UUID classroomId = classroom.getId();
        final UUID teacherId   = teacher.getId();
        assertThrows(SchoolMismatchException.class, () -> this.adapter.addTeacher(classroomId, teacherId));
        verify(this.classroomRepository, never()).save(any());
    }

    @Test
    void addTeacherAuthorizesAddsAndSavesWhenValid() {
        final School    school    = aSchool();
        final Classroom classroom = aClassroom(school);
        final Teacher   teacher   = aTeacherAt(school);
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.teacherRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        assertTrue(this.adapter.addTeacher(classroom.getId(), teacher.getId()));
        verify(this.policy).authorizeUpdate(classroom);
        assertTrue(classroom.getTeachers().contains(teacher));
        verify(this.classroomRepository).save(classroom);
    }

    @Test
    void addTeacherPropagatesAnUnauthorizedRejectionWithoutSaving() {
        final School    school    = aSchool();
        final Classroom classroom = aClassroom(school);
        final Teacher   teacher   = aTeacherAt(school);
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.teacherRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeUpdate(classroom);
        final UUID classroomId = classroom.getId();
        final UUID teacherId   = teacher.getId();
        assertThrows(AccessDeniedException.class, () -> this.adapter.addTeacher(classroomId, teacherId));
        verify(this.classroomRepository, never()).save(any());
    }

    @Test
    void setTutorReturnsFalseWhenTheClassroomDoesNotExist() {
        final UUID    classroomId = UUID.randomUUID();
        final Teacher teacher     = aTeacherAt(aSchool());
        when(this.classroomRepository.findById(classroomId)).thenReturn(Optional.empty());
        when(this.teacherRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        assertFalse(this.adapter.setTutor(classroomId, teacher.getId()));
        verify(this.classroomRepository, never()).save(any());
    }

    @Test
    void setTutorReturnsFalseWhenTheTeacherDoesNotExist() {
        final Classroom classroom = aClassroom(aSchool());
        final UUID      teacherId = UUID.randomUUID();
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.teacherRepository.findById(teacherId)).thenReturn(Optional.empty());
        assertFalse(this.adapter.setTutor(classroom.getId(), teacherId));
        verify(this.classroomRepository, never()).save(any());
    }

    @Test
    void setTutorFailsWhenTheProposedTutorBelongsToADifferentSchool() {
        final School    school    = aSchool();
        final Classroom classroom = aClassroom(school);
        final Teacher   teacher   = aTeacherAt(aSchool());
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.teacherRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        final UUID classroomId = classroom.getId();
        final UUID teacherId   = teacher.getId();
        assertThrows(SchoolMismatchException.class, () -> this.adapter.setTutor(classroomId, teacherId));
        verify(this.classroomRepository, never()).save(any());
    }

    @Test
    void setTutorAuthorizesSetsAndSavesWhenValid() {
        final School    school    = aSchool();
        final Classroom classroom = aClassroom(school);
        final Teacher   teacher   = aTeacherAt(school);
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.teacherRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        assertTrue(this.adapter.setTutor(classroom.getId(), teacher.getId()));
        verify(this.policy).authorizeUpdate(classroom);
        assertEquals(teacher, classroom.getTutor());
        verify(this.classroomRepository).save(classroom);
    }

    @Test
    void setTutorAuthorizesAgainstTheOutgoingTutorBeforeOverwritingIt() {
        final School    school        = aSchool();
        final Classroom classroom     = aClassroom(school);
        final Teacher   outgoingTutor = aTeacherAt(school);
        final Teacher   proposedTutor = aTeacherAt(school);
        classroom.setTutor(outgoingTutor);
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.teacherRepository.findById(proposedTutor.getId())).thenReturn(Optional.of(proposedTutor));
        // Classroom is mutable and Mockito verify() inspects arguments by reference, not a snapshot at
        // call time, so the tutor seen during authorization is captured live, inside the stub, before
        // setTutor() has a chance to overwrite it.
        final AtomicReference<Teacher> tutorSeenDuringAuthorization = new AtomicReference<>();
        doAnswer(invocation -> {
            tutorSeenDuringAuthorization.set(((Classroom) invocation.getArgument(0)).getTutor());
            return null;
        }).when(this.policy).authorizeUpdate(any());
        this.adapter.setTutor(classroom.getId(), proposedTutor.getId());
        assertEquals(outgoingTutor, tutorSeenDuringAuthorization.get());
    }

    @Test
    void setTutorPropagatesAnUnauthorizedRejectionWithoutSaving() {
        final School    school    = aSchool();
        final Classroom classroom = aClassroom(school);
        final Teacher   teacher   = aTeacherAt(school);
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.teacherRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeUpdate(classroom);
        final UUID classroomId = classroom.getId();
        final UUID teacherId   = teacher.getId();
        assertThrows(AccessDeniedException.class, () -> this.adapter.setTutor(classroomId, teacherId));
        verify(this.classroomRepository, never()).save(any());
        assertNull(classroom.getTutor());
    }

    @Test
    void enrollKidReturnsFalseWhenTheClassroomDoesNotExist() {
        final UUID classroomId = UUID.randomUUID();
        final Kid  kid         = new Kid();
        ReflectionTestUtils.setField(kid, "id", UUID.randomUUID());
        when(this.classroomRepository.findById(classroomId)).thenReturn(Optional.empty());
        when(this.kidRepository.findById(kid.getId())).thenReturn(Optional.of(kid));
        assertFalse(this.adapter.enrollKid(classroomId, kid.getId()));
        verify(this.kidRepository, never()).save(any());
    }

    @Test
    void enrollKidReturnsFalseWhenTheKidDoesNotExist() {
        final Classroom classroom = aClassroom(aSchool());
        final UUID      kidId     = UUID.randomUUID();
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.kidRepository.findById(kidId)).thenReturn(Optional.empty());
        assertFalse(this.adapter.enrollKid(classroom.getId(), kidId));
        verify(this.kidRepository, never()).save(any());
    }

    @Test
    void enrollKidAuthorizesSetsAndSavesWhenValid() {
        final Classroom classroom = aClassroom(aSchool());
        final Kid       kid       = new Kid();
        ReflectionTestUtils.setField(kid, "id", UUID.randomUUID());
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.kidRepository.findById(kid.getId())).thenReturn(Optional.of(kid));
        assertTrue(this.adapter.enrollKid(classroom.getId(), kid.getId()));
        verify(this.policy).authorizeUpdate(classroom);
        assertEquals(classroom, kid.getClassroom());
        verify(this.kidRepository).save(kid);
    }

    @Test
    void enrollKidPropagatesAnUnauthorizedRejectionWithoutSaving() {
        final Classroom classroom = aClassroom(aSchool());
        final Kid       kid       = new Kid();
        ReflectionTestUtils.setField(kid, "id", UUID.randomUUID());
        when(this.classroomRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(this.kidRepository.findById(kid.getId())).thenReturn(Optional.of(kid));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeUpdate(classroom);
        final UUID classroomId = classroom.getId();
        final UUID kidId       = kid.getId();
        assertThrows(AccessDeniedException.class, () -> this.adapter.enrollKid(classroomId, kidId));
        verify(this.kidRepository, never()).save(any());
    }

    @Test
    void findByKidReturnsEmptyWhenTheKidDoesNotExist() {
        final UUID kidId = UUID.randomUUID();
        when(this.kidRepository.findById(kidId)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.findByKid(kidId));
    }

    @Test
    void findByKidReturnsEmptyWhenTheKidHasNoClassroom() {
        final Kid kid = new Kid();
        ReflectionTestUtils.setField(kid, "id", UUID.randomUUID());
        when(this.kidRepository.findById(kid.getId())).thenReturn(Optional.of(kid));
        assertEquals(Optional.empty(), this.adapter.findByKid(kid.getId()));
    }

    @Test
    void findByKidReturnsTheClassroom() {
        final Classroom classroom = aClassroom(aSchool());
        final Kid       kid       = new Kid();
        ReflectionTestUtils.setField(kid, "id", UUID.randomUUID());
        kid.setClassroom(classroom);
        when(this.kidRepository.findById(kid.getId())).thenReturn(Optional.of(kid));
        assertEquals(3, this.adapter.findByKid(kid.getId()).orElseThrow().getCourse());
    }

    @Test
    void findBySchoolReturnsEmptyWhenTheSchoolDoesNotExist() {
        final UUID schoolId = UUID.randomUUID();
        when(this.schoolRepository.existsById(schoolId)).thenReturn(false);
        final Pageable pageable = PageRequest.of(0, 20);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Classroom> pagedAssembler = mock(PagedResourcesAssembler.class);
        assertEquals(Optional.empty(), this.adapter.findBySchool(schoolId, pageable, pagedAssembler));
    }

    @Test
    void findBySchoolReturnsThePagedClassrooms() {
        final School school   = aSchool();
        final UUID   schoolId = school.getId();
        when(this.schoolRepository.existsById(schoolId)).thenReturn(true);
        final Pageable pageable = PageRequest.of(0, 20);
        final Page<Classroom> page = new PageImpl<>(List.of(aClassroom(school)));
        when(this.classroomRepository.findBySchoolId(schoolId, pageable)).thenReturn(page);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Classroom> pagedAssembler = mock(PagedResourcesAssembler.class);
        final PagedModel<ClassroomModel> expected = PagedModel.empty();
        when(pagedAssembler.toModel(page, this.adapter)).thenReturn(expected);
        assertEquals(expected, this.adapter.findBySchool(schoolId, pageable, pagedAssembler).orElseThrow());
    }

}
