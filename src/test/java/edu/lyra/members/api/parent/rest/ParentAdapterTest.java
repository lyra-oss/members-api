package edu.lyra.members.api.parent.rest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
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
class ParentAdapterTest {

    private final ParentMapper mapper = Mappers.getMapper(ParentMapper.class);
    @Mock
    private ParentRepository parentRepository;
    @Mock
    private KidRepository kidRepository;
    @Mock
    private PersonRepository personRepository;
    private ParentPolicy policy;

    private ParentAdapter adapter;

    private static Kid aKid() {
        final Kid kid = new Kid();
        ReflectionTestUtils.setField(kid, "id", UUID.randomUUID());
        return kid;
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        this.policy  = mock(ParentPolicy.class);
        this.adapter = new ParentAdapter(this.parentRepository, this.kidRepository, this.personRepository, this.mapper,
                                         this.policy);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    private static Parent aParent(final String name) {
        final Parent parent = new Parent();
        parent.setName(name);
        parent.setSurname("Cristóbal");
        parent.setMail("esteban.cristobal@example.com");
        ReflectionTestUtils.setField(parent, "id", UUID.randomUUID());
        return parent;
    }

    @Test
    void findByIdReturnsEmptyWhenTheParentDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.parentRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.findById(id));
    }

    @Test
    void findByIdReturnsTheModelWhenTheParentExists() {
        final UUID   id     = UUID.randomUUID();
        final Parent parent = aParent("Esteban");
        when(this.parentRepository.findById(id)).thenReturn(Optional.of(parent));
        assertEquals("Esteban", this.adapter.findById(id).orElseThrow().getName());
    }

    @Test
    void toModelAddsASelfLink() {
        final Parent      parent = aParent("Esteban");
        final ParentModel model  = this.adapter.toModel(parent);
        assertEquals("Esteban", model.getName());
        assertTrue(model.getRequiredLink("self").getHref().endsWith("/parents/" + parent.getId()));
    }

    @Test
    void findAllDelegatesToThePagedResourcesAssembler() {
        final Pageable     pageable = PageRequest.of(0, 20);
        final Page<Parent> page     = new PageImpl<>(List.of(aParent("Esteban")));
        when(this.parentRepository.findAll(pageable)).thenReturn(page);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<Parent> pagedAssembler = mock(PagedResourcesAssembler.class);
        final PagedModel<ParentModel> expected = PagedModel.empty();
        when(pagedAssembler.toModel(page, this.adapter)).thenReturn(expected);
        assertEquals(expected, this.adapter.findAll(pageable, pagedAssembler));
    }

    @Test
    void createUsesTheExistingPersonWhenTheAuthenticatedSubjectIsAlreadyRegistered() {
        final UUID subject = UUID.randomUUID();
        authenticateAs(subject);
        final Person existingPerson = Person.builder().id(subject).name("Already").surname("Registered")
                                            .mail("already.registered@example.com").build();
        when(this.personRepository.findById(subject)).thenReturn(Optional.of(existingPerson));
        when(this.parentRepository.save(any(Parent.class))).thenAnswer(inv -> inv.getArgument(0));
        final ParentRequest request = new ParentRequest("Esteban", "Cristóbal", "esteban.cristobal@example.com");
        final ParentModel model = this.adapter.create(request);
        assertEquals("Already", model.getName());
        assertEquals("Registered", model.getSurname());
    }

    private static void authenticateAs(final UUID id) {
        final Jwt            jwt            =
                Jwt.withTokenValue("token").header("alg", "none").subject(id.toString()).build();
        final Authentication authentication = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void createBuildsANewPersonUnderTheSubjectWhenNonePreviouslyExisted() {
        final UUID subject = UUID.randomUUID();
        authenticateAs(subject);
        when(this.personRepository.findById(subject)).thenReturn(Optional.empty());
        when(this.parentRepository.save(any(Parent.class))).thenAnswer(inv -> inv.getArgument(0));
        final ParentRequest request = new ParentRequest("Esteban", "Cristóbal", "esteban.cristobal@example.com");
        final ParentModel model = this.adapter.create(request);
        assertEquals("Esteban", model.getName());
        final ArgumentCaptor<Parent> saved = ArgumentCaptor.forClass(Parent.class);
        verify(this.parentRepository).save(saved.capture());
        assertEquals(subject, saved.getValue().getPerson().getId());
    }

    @Test
    void updateAuthorizesBeforeSaving() {
        final UUID   id     = UUID.randomUUID();
        final Parent parent = aParent("Esteban");
        when(this.parentRepository.findById(id)).thenReturn(Optional.of(parent));
        when(this.parentRepository.save(parent)).thenReturn(parent);
        final ParentModel model =
                this.adapter.update(id, new ParentPatchRequest(null, "New surname", null)).orElseThrow();
        verify(this.policy).authorizeUpdate(parent);
        assertEquals("New surname", model.getSurname());
    }

    @Test
    void updatePropagatesAnUnauthorizedRejectionWithoutSaving() {
        final UUID   id     = UUID.randomUUID();
        final Parent parent = aParent("Esteban");
        when(this.parentRepository.findById(id)).thenReturn(Optional.of(parent));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeUpdate(parent);
        final ParentPatchRequest request = new ParentPatchRequest(null, "New surname", null);
        assertThrows(AccessDeniedException.class, () -> this.adapter.update(id, request));
        verify(this.parentRepository, never()).save(any());
    }

    @Test
    void deleteReturnsFalseWhenTheParentDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.parentRepository.findById(id)).thenReturn(Optional.empty());
        assertFalse(this.adapter.delete(id));
    }

    @Test
    void deleteAuthorizesBeforeDeleting() {
        final UUID   id     = UUID.randomUUID();
        final Parent parent = aParent("Esteban");
        when(this.parentRepository.findById(id)).thenReturn(Optional.of(parent));
        assertTrue(this.adapter.delete(id));
        verify(this.policy).authorizeDelete(parent);
        verify(this.parentRepository).delete(parent);
    }

    @Test
    void deletePropagatesAnUnauthorizedRejectionWithoutDeleting() {
        final UUID   id     = UUID.randomUUID();
        final Parent parent = aParent("Esteban");
        when(this.parentRepository.findById(id)).thenReturn(Optional.of(parent));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeDelete(parent);
        assertThrows(AccessDeniedException.class, () -> this.adapter.delete(id));
        verify(this.parentRepository, never()).delete(any());
    }

    @Test
    void bindKidReturnsFalseWhenTheParentDoesNotExist() {
        final UUID parentId = UUID.randomUUID();
        final UUID kidId    = UUID.randomUUID();
        when(this.parentRepository.findById(parentId)).thenReturn(Optional.empty());
        when(this.kidRepository.findById(kidId)).thenReturn(Optional.of(aKid()));
        assertFalse(this.adapter.bindKid(parentId, kidId));
        verify(this.kidRepository, never()).save(any());
    }

    @Test
    void updateReturnsEmptyWhenTheParentDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.parentRepository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.update(id, new ParentPatchRequest(null, "New surname", null)));
    }

    @Test
    void bindKidReturnsFalseWhenTheKidDoesNotExist() {
        final UUID parentId = UUID.randomUUID();
        final UUID kidId    = UUID.randomUUID();
        when(this.parentRepository.findById(parentId)).thenReturn(Optional.of(aParent("Esteban")));
        when(this.kidRepository.findById(kidId)).thenReturn(Optional.empty());
        assertFalse(this.adapter.bindKid(parentId, kidId));
        verify(this.kidRepository, never()).save(any());
    }

    @Test
    void bindKidAuthorizesBeforeSaving() {
        final Parent parent = aParent("Esteban");
        final Kid    kid    = aKid();
        when(this.parentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(this.kidRepository.findById(kid.getId())).thenReturn(Optional.of(kid));
        assertTrue(this.adapter.bindKid(parent.getId(), kid.getId()));
        verify(this.policy).authorizeKidBinding(parent, kid);
        assertEquals(parent, kid.getParent());
        verify(this.kidRepository).save(kid);
    }

    @Test
    void bindKidPropagatesAnUnauthorizedRejectionWithoutSaving() {
        final Parent parent = aParent("Esteban");
        final Kid    kid    = aKid();
        when(this.parentRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(this.kidRepository.findById(kid.getId())).thenReturn(Optional.of(kid));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeKidBinding(parent, kid);
        final UUID parentId = parent.getId();
        final UUID kidId    = kid.getId();
        assertThrows(AccessDeniedException.class, () -> this.adapter.bindKid(parentId, kidId));
        verify(this.kidRepository, never()).save(any());
    }

    @Test
    void findByKidReturnsEmptyWhenTheKidDoesNotExist() {
        final UUID kidId = UUID.randomUUID();
        when(this.kidRepository.findById(kidId)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.findByKid(kidId));
    }

    @Test
    void findByKidReturnsEmptyWhenTheKidHasNoParent() {
        final Kid kid = aKid();
        when(this.kidRepository.findById(kid.getId())).thenReturn(Optional.of(kid));
        assertEquals(Optional.empty(), this.adapter.findByKid(kid.getId()));
    }

    @Test
    void findByKidReturnsTheParent() {
        final Parent parent = aParent("Esteban");
        final Kid    kid    = aKid();
        kid.setParent(parent);
        when(this.kidRepository.findById(kid.getId())).thenReturn(Optional.of(kid));
        assertEquals("Esteban", this.adapter.findByKid(kid.getId()).orElseThrow().getName());
    }

}
