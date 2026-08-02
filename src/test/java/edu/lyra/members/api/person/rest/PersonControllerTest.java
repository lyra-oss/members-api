package edu.lyra.members.api.person.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.person.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonControllerTest {

    @Mock
    private PersonAdapter adapter;

    @Mock
    private PagedResourcesAssembler<Person> pagedAssembler;

    private PersonController controller;

    @BeforeEach
    void setUp() {
        this.controller = new PersonController(this.adapter, this.pagedAssembler);
    }

    private static PersonModel aModel(final UUID id) {
        return new PersonModel(id, "Esteban", "Cristóbal", "esteban.cristobal@example.com");
    }

    @Test
    void findAllDelegatesToTheAdapter() {
        final Pageable pageable = Pageable.unpaged();
        final PagedModel<PersonModel> expected = PagedModel.empty();
        when(this.adapter.findAll(pageable, this.pagedAssembler)).thenReturn(expected);
        assertEquals(expected, this.controller.findAll(pageable));
    }

    @Test
    void getReturnsOkWhenThePersonExists() {
        final UUID id = UUID.randomUUID();
        final PersonModel model = aModel(id);
        when(this.adapter.findById(id)).thenReturn(Optional.of(model));
        final ResponseEntity<PersonModel> response = this.controller.get(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(model, response.getBody());
    }

    @Test
    void getReturnsNotFoundWhenThePersonIsMissing() {
        final UUID id = UUID.randomUUID();
        when(this.adapter.findById(id)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.get(id).getStatusCode());
    }

    @Test
    void grantTeacherRoleReturnsNotFoundWhenThePersonIsMissing() {
        final UUID id = UUID.randomUUID();
        final GrantTeacherRoleRequest request = new GrantTeacherRoleRequest(UUID.randomUUID());
        when(this.adapter.grantTeacherRole(id, request)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, this.controller.grantTeacherRole(id, request).getStatusCode());
    }

    @Test
    void revokeTeacherRoleReturnsNotFoundWhenThePersonDidNotHoldTheRole() {
        final UUID id = UUID.randomUUID();
        when(this.adapter.revokeTeacherRole(id)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, this.controller.revokeTeacherRole(id).getStatusCode());
    }

}
