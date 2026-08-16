package edu.lyra.members.api.kid.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.exceptions.UnresolvableReferenceException;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
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
class KidAdapterTest {

    private final KidMapper mapper = Mappers.getMapper(KidMapper.class);
    @Mock
    private KidRepository kidRepository;
    @Mock
    private ParentRepository parentRepository;
    @Mock
    private ClassroomRepository classroomRepository;
    private KidPolicy policy;

    private KidVisibilityStrategyResolver visibilityResolver;

    private KidAdapter adapter;

    @BeforeEach
    void setUp() {
        this.policy             = mock(KidPolicy.class);
        this.visibilityResolver = mock(KidVisibilityStrategyResolver.class);
        //@formatter:off
        this.adapter = new KidAdapter(this.kidRepository, this.parentRepository, this.classroomRepository,
                                      this.visibilityResolver, this.mapper, this.policy);
        //@formatter:on
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    private static Parent aParent(final UUID id) {
        final Parent parent = new Parent();
        ReflectionTestUtils.setField(parent, "id", id);
        return parent;
    }

    private static Kid aKid(final String name) {
        final Kid kid = new Kid();
        kid.setName(name);
        kid.setSurname("Cristóbal");
        kid.setBirthdate(LocalDate.of(2019, 12, 12));
        ReflectionTestUtils.setField(kid, "id", UUID.randomUUID());
        return kid;
    }

    @Test
    void findByIdReturnsEmptyWhenTheKidDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.kidRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.findById(id));
    }

    @Test
    void findByIdReturnsTheModelWhenTheKidExists() {
        final UUID id  = UUID.randomUUID();
        final Kid  kid = aKid("Alicia");
        when(this.kidRepository.findById(id)).thenReturn(Optional.of(kid));
        assertEquals("Alicia", this.adapter.findById(id).orElseThrow().getName());
    }

    private static void authenticateAs(final UUID id) {
        final Jwt            jwt            =
                Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final Authentication authentication = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void createAssignsTheAuthenticatedParent() {
        final UUID   subject = UUID.randomUUID();
        final Parent parent  = aParent(subject);
        authenticateAs(subject);
        when(this.policy.authorizeCreate(subject)).thenReturn(parent);
        when(this.kidRepository.save(any(Kid.class))).thenAnswer(inv -> inv.getArgument(0));
        final KidRequest request = new KidRequest("Alicia", "Cristóbal", LocalDate.of(2019, 12, 12));
        final KidModel model = this.adapter.create(request);
        assertEquals("Alicia", model.getName());
        final ArgumentCaptor<Kid> saved = ArgumentCaptor.forClass(Kid.class);
        verify(this.kidRepository).save(saved.capture());
        assertEquals(parent, saved.getValue().getParent());
    }

    private static Classroom aClassroom() {
        final Classroom classroom = new Classroom();
        ReflectionTestUtils.setField(classroom, "id", UUID.randomUUID());
        return classroom;
    }

    @Test
    void toModelAddsASelfLink() {
        final Kid      kid   = aKid("Alicia");
        final KidModel model = this.adapter.toModel(kid);
        assertEquals("Alicia", model.getName());
        assertTrue(model.getRequiredLink("self").getHref().endsWith("/kids/" + kid.getId()));
    }

    @Test
    void createRejectsWhenTheAuthenticatedSubjectIsNotARegisteredParent() {
        final UUID subject = UUID.randomUUID();
        authenticateAs(subject);
        when(this.policy.authorizeCreate(subject)).thenThrow(
                new AccessDeniedException("Authenticated user cannot register this kid"));
        final KidRequest request = new KidRequest("Alicia", "Cristóbal", LocalDate.of(2019, 12, 12));
        assertThrows(AccessDeniedException.class, () -> this.adapter.create(request));
        verify(this.kidRepository, never()).save(any());
    }

    @Test
    void findAllDelegatesToTheVisibilityResolverAndThePagedResourcesAssembler() {
        final Pageable  pageable = PageRequest.of(0, 20);
        final Page<Kid> page     = new PageImpl<>(List.of(aKid("Alicia")));
        when(this.visibilityResolver.resolve(pageable)).thenReturn(page);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Kid> pagedAssembler = mock(PagedResourcesAssembler.class);
        final PagedModel<KidModel> expected = PagedModel.empty();
        when(pagedAssembler.toModel(page, this.adapter)).thenReturn(expected);
        assertEquals(expected, this.adapter.findAll(pageable, pagedAssembler));
        verify(this.kidRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void updateAuthorizesAgainstTheUnchangedParentAndClassroomForAPlainFieldUpdate() {
        final UUID   id     = UUID.randomUUID();
        final Kid    kid    = aKid("Alicia");
        final Parent parent = aParent(UUID.randomUUID());
        kid.setParent(parent);
        when(this.kidRepository.findById(id)).thenReturn(Optional.of(kid));
        when(this.kidRepository.save(kid)).thenReturn(kid);
        final KidPatchRequest request = new KidPatchRequest(null, "New surname", null, null, null);
        final KidModel model = this.adapter.update(id, request).orElseThrow();
        verify(this.policy).authorizeUpdate(kid, parent, null);
        assertEquals("New surname", model.getSurname());
    }

    @Test
    void updateKeepsTheExistingClassroomWhenTheRequestDoesNotChangeIt() {
        final UUID      id        = UUID.randomUUID();
        final Kid       kid       = aKid("Alicia");
        final Classroom classroom = aClassroom();
        kid.setClassroom(classroom);
        when(this.kidRepository.findById(id)).thenReturn(Optional.of(kid));
        when(this.kidRepository.save(kid)).thenReturn(kid);
        final KidPatchRequest request = new KidPatchRequest(null, "New surname", null, null, null);
        this.adapter.update(id, request);
        verify(this.policy).authorizeUpdate(kid, null, classroom);
        assertEquals(classroom, kid.getClassroom());
    }

    @Test
    void updateReturnsEmptyWhenTheKidDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.kidRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(),
                     this.adapter.update(id, new KidPatchRequest(null, "New surname", null, null, null)));
    }

    @Test
    void updatePropagatesAnUnauthorizedRejectionWithoutSaving() {
        final UUID id  = UUID.randomUUID();
        final Kid  kid = aKid("Alicia");
        when(this.kidRepository.findById(id)).thenReturn(Optional.of(kid));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeUpdate(kid, null, null);
        final KidPatchRequest request = new KidPatchRequest(null, "New surname", null, null, null);
        assertThrows(AccessDeniedException.class, () -> this.adapter.update(id, request));
        verify(this.kidRepository, never()).save(any());
    }

    @Test
    void updateResolvesAndAppliesANewParent() {
        final UUID   id        = UUID.randomUUID();
        final Kid    kid       = aKid("Alicia");
        final Parent newParent = aParent(UUID.randomUUID());
        when(this.kidRepository.findById(id)).thenReturn(Optional.of(kid));
        when(this.parentRepository.findById(newParent.getId())).thenReturn(Optional.of(newParent));
        when(this.kidRepository.save(kid)).thenReturn(kid);
        final KidPatchRequest request = new KidPatchRequest(null, null, null, newParent.getId(), null);
        this.adapter.update(id, request);
        verify(this.policy).authorizeUpdate(kid, newParent, null);
        assertEquals(newParent, kid.getParent());
    }

    @Test
    void updateFailsWithAnUnresolvableReferenceWhenTheNewParentDoesNotExist() {
        final UUID id            = UUID.randomUUID();
        final Kid  kid           = aKid("Alicia");
        final UUID unknownParent = UUID.randomUUID();
        when(this.kidRepository.findById(id)).thenReturn(Optional.of(kid));
        when(this.parentRepository.findById(unknownParent)).thenReturn(Optional.empty());
        final KidPatchRequest request = new KidPatchRequest(null, null, null, unknownParent, null);
        assertThrows(UnresolvableReferenceException.class, () -> this.adapter.update(id, request));
        verify(this.kidRepository, never()).save(any());
    }

    @Test
    void updateResolvesAndAppliesANewClassroom() {
        final UUID      id           = UUID.randomUUID();
        final Kid       kid          = aKid("Alicia");
        final Classroom newClassroom = aClassroom();
        when(this.kidRepository.findById(id)).thenReturn(Optional.of(kid));
        when(this.classroomRepository.findById(newClassroom.getId())).thenReturn(Optional.of(newClassroom));
        when(this.kidRepository.save(kid)).thenReturn(kid);
        final KidPatchRequest request = new KidPatchRequest(null, null, null, null, newClassroom.getId());
        this.adapter.update(id, request);
        verify(this.policy).authorizeUpdate(kid, null, newClassroom);
        assertEquals(newClassroom, kid.getClassroom());
    }

    @Test
    void updateFailsWithAnUnresolvableReferenceWhenTheNewClassroomDoesNotExist() {
        final UUID id               = UUID.randomUUID();
        final Kid  kid              = aKid("Alicia");
        final UUID unknownClassroom = UUID.randomUUID();
        when(this.kidRepository.findById(id)).thenReturn(Optional.of(kid));
        when(this.classroomRepository.findById(unknownClassroom)).thenReturn(Optional.empty());
        final KidPatchRequest request = new KidPatchRequest(null, null, null, null, unknownClassroom);
        assertThrows(UnresolvableReferenceException.class, () -> this.adapter.update(id, request));
        verify(this.kidRepository, never()).save(any());
    }

    @Test
    void deleteReturnsFalseWhenTheKidDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.kidRepository.findById(id)).thenReturn(Optional.empty());
        assertFalse(this.adapter.delete(id));
    }

    @Test
    void deleteAuthorizesBeforeDeleting() {
        final UUID id  = UUID.randomUUID();
        final Kid  kid = aKid("Alicia");
        when(this.kidRepository.findById(id)).thenReturn(Optional.of(kid));
        assertTrue(this.adapter.delete(id));
        verify(this.policy).authorizeDelete(kid);
        verify(this.kidRepository).delete(kid);
    }

    @Test
    void deletePropagatesAnUnauthorizedRejectionWithoutDeleting() {
        final UUID id  = UUID.randomUUID();
        final Kid  kid = aKid("Alicia");
        when(this.kidRepository.findById(id)).thenReturn(Optional.of(kid));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeDelete(kid);
        assertThrows(AccessDeniedException.class, () -> this.adapter.delete(id));
        verify(this.kidRepository, never()).delete(any());
    }

    @Test
    void findByParentReturnsEmptyWhenTheParentDoesNotExist() {
        final UUID parentId = UUID.randomUUID();
        when(this.parentRepository.existsById(parentId)).thenReturn(false);
        final Pageable pageable = PageRequest.of(0, 20);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Kid> pagedAssembler = mock(PagedResourcesAssembler.class);
        assertEquals(Optional.empty(), this.adapter.findByParent(parentId, pageable, pagedAssembler));
    }

    @Test
    void findByParentReturnsThePagedKids() {
        final UUID parentId = UUID.randomUUID();
        when(this.parentRepository.existsById(parentId)).thenReturn(true);
        final Pageable  pageable = PageRequest.of(0, 20);
        final Page<Kid> page     = new PageImpl<>(List.of(aKid("Alicia")));
        when(this.kidRepository.findByParentIdOrderByNameAsc(parentId, pageable)).thenReturn(page);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Kid> pagedAssembler = mock(PagedResourcesAssembler.class);
        final PagedModel<KidModel> expected = PagedModel.empty();
        when(pagedAssembler.toModel(page, this.adapter)).thenReturn(expected);
        assertEquals(expected, this.adapter.findByParent(parentId, pageable, pagedAssembler).orElseThrow());
    }

}
