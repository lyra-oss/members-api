package edu.lyra.members.api.exceptions;

import lombok.experimental.StandardException;

/**
 * Thrown when a teacher is deleted, or has the teacher role revoked, while still tutoring or teaching a classroom.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@StandardException
public class TeacherAssignedToClassroomException
        extends RuntimeException {}
