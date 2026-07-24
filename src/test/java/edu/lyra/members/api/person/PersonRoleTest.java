package edu.lyra.members.api.person;

import java.util.UUID;

import edu.lyra.members.api.parent.Parent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PersonRoleTest {

    @Test
    void getIdReturnsTheAssignedId() {
        final UUID id = UUID.randomUUID();
        assertEquals(id, Parent.builder().id(id).build().getId());
    }

    @Test
    void getIdReturnsNullForATransientInstance() {
        assertNull(Parent.builder().build().getId());
    }

}
