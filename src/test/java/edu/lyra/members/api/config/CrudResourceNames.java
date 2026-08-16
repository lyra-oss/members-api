package edu.lyra.members.api.config;

import java.util.List;
import java.util.stream.Stream;

public final class CrudResourceNames {

    public static final List<String> ALL = List.of("parents", "kids", "schools", "teachers", "classrooms");

    private CrudResourceNames() {
    }

    public static Stream<String> stream() {
        return ALL.stream();
    }

}
