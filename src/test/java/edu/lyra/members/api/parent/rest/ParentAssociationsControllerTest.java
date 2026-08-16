package edu.lyra.members.api.parent.rest;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentAssociationsControllerTest {

    @Mock
    private ParentAdapter adapter;

    private ParentAssociationsController controller;

    @BeforeEach
    void setUp() {
        this.controller = new ParentAssociationsController(this.adapter);
    }

    @Test
    void findByKidReturnsOkWhenTheKidsParentExists() {
        final UUID kidId = UUID.randomUUID();
        final ParentModel model =
                new ParentModel(UUID.randomUUID(), "Esteban", "Cristóbal", "esteban.cristobal@example.com");
        when(this.adapter.findByKid(kidId)).thenReturn(Optional.of(model));
        final ResponseEntity<ParentModel> response = this.controller.findByKid(kidId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(model, response.getBody());
    }

    @Test
    void findByKidReturnsNotFoundWhenTheKidOrItsParentIsMissing() {
        final UUID kidId = UUID.randomUUID();
        when(this.adapter.findByKid(kidId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.findByKid(kidId).getStatusCode());
    }

}
