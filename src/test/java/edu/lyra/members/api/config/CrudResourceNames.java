package edu.lyra.members.api.config;

import java.util.List;
import java.util.stream.Stream;

// The five aggregates exposed with full create/read/update/delete through the REST API. Person is
// deliberately excluded: it only supports read plus role grant/revoke, a different shape entirely.
// Shared by tests asserting something uniformly true across all five, to avoid repeating the literal.
public final class CrudResourceNames {

    public static final List<String> ALL = List.of("parents", "kids", "schools", "teachers", "classrooms");

    private CrudResourceNames() {}

    public static Stream<String> stream() {
        return ALL.stream();
    }

}
