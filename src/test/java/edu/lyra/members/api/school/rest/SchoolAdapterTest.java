package edu.lyra.members.api.school.rest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.config.web.ApiBasePath;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolRepository;
import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAdapterTest {

    @Mock
    private SchoolRepository repository;

    private final SchoolMapper mapper = Mappers.getMapper(SchoolMapper.class);

    private SchoolPolicy policy;

    private SchoolAdapter adapter;

    @BeforeEach
    void setUp() {
        this.policy  = mock(SchoolPolicy.class);
        this.adapter = new SchoolAdapter(this.repository, this.mapper, this.policy, new ApiBasePath("/v0"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static School aSchool(final String name) {
        final School school = new School();
        school.setName(name);
        // id is @GeneratedValue and only ever populated by JPA on a real save; set it directly here so
        // link-building tests (which never persist) still have something to build a self href from.
        ReflectionTestUtils.setField(school, "id", UUID.randomUUID());
        return school;
    }

    @Test
    void toModelAddsASelfLinkPrefixedWithTheApiBasePath() {
        final School school = aSchool("Gloria Fuertes");
        final SchoolModel model = this.adapter.toModel(school);
        assertEquals("Gloria Fuertes", model.getName());
        final String href = model.getRequiredLink("self").getHref();
        assertTrue(href.endsWith("/v0/schools/" + school.getId()), "unexpected href: " + href);
    }

    @Test
    void findByIdReturnsEmptyWhenTheSchoolDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.repository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.findById(id));
    }

    @Test
    void findByIdReturnsTheModelWhenTheSchoolExists() {
        final UUID   id     = UUID.randomUUID();
        final School school = aSchool("Gloria Fuertes");
        when(this.repository.findById(id)).thenReturn(Optional.of(school));
        assertEquals("Gloria Fuertes", this.adapter.findById(id).orElseThrow().getName());
    }

    @Test
    void findAllDelegatesToThePagedResourcesAssembler() {
        final Pageable pageable = PageRequest.of(0, 20);
        final Page<School> page = new PageImpl<>(List.of(aSchool("Gloria Fuertes")));
        when(this.repository.findAll(pageable)).thenReturn(page);
        @SuppressWarnings("unchecked")
        final PagedResourcesAssembler<School> pagedAssembler = mock(PagedResourcesAssembler.class);
        final PagedModel<SchoolModel> expected = PagedModel.empty();
        when(pagedAssembler.toModel(page, this.adapter)).thenReturn(expected);
        assertEquals(expected, this.adapter.findAll(pageable, pagedAssembler));
    }

    @Test
    void createMapsAndSavesTheRequest() {
        final School saved = aSchool("Gloria Fuertes");
        when(this.repository.save(any(School.class))).thenReturn(saved);
        final SchoolModel model = this.adapter.create(new SchoolRequest("Gloria Fuertes"));
        assertEquals("Gloria Fuertes", model.getName());
    }

    @Test
    void updateReturnsEmptyWhenTheSchoolDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.repository.findById(id)).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), this.adapter.update(id, new SchoolRequest("New name")));
    }

    @Test
    void updateAuthorizesBeforeSaving() {
        final UUID   id     = UUID.randomUUID();
        final School school = aSchool("Old name");
        when(this.repository.findById(id)).thenReturn(Optional.of(school));
        when(this.repository.save(school)).thenReturn(school);
        final SchoolModel model = this.adapter.update(id, new SchoolRequest("New name")).orElseThrow();
        verify(this.policy).authorizeUpdate(school);
        assertEquals("New name", model.getName());
    }

    @Test
    void updatePropagatesAnUnauthorizedRejectionWithoutSaving() {
        final UUID   id     = UUID.randomUUID();
        final School school = aSchool("Old name");
        when(this.repository.findById(id)).thenReturn(Optional.of(school));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeUpdate(school);
        assertThrows(AccessDeniedException.class, () -> this.adapter.update(id, new SchoolRequest("New name")));
        verify(this.repository, never()).save(any());
    }

    @Test
    void deleteReturnsFalseWhenTheSchoolDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.repository.findById(id)).thenReturn(Optional.empty());
        assertFalse(this.adapter.delete(id));
    }

    @Test
    void deleteAuthorizesBeforeDeleting() {
        final UUID   id     = UUID.randomUUID();
        final School school = aSchool("Gloria Fuertes");
        when(this.repository.findById(id)).thenReturn(Optional.of(school));
        assertTrue(this.adapter.delete(id));
        verify(this.policy).authorizeDelete(school);
        verify(this.repository).delete(school);
    }

    @Test
    void deletePropagatesAnUnauthorizedRejectionWithoutDeleting() {
        final UUID   id     = UUID.randomUUID();
        final School school = aSchool("Gloria Fuertes");
        when(this.repository.findById(id)).thenReturn(Optional.of(school));
        doThrow(new AccessDeniedException("nope")).when(this.policy).authorizeDelete(school);
        assertThrows(AccessDeniedException.class, () -> this.adapter.delete(id));
        verify(this.repository, never()).delete(any());
    }

}
