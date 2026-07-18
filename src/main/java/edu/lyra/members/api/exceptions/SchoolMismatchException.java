package edu.lyra.members.api.exceptions;

import lombok.experimental.StandardException;

/**
 * Thrown when a teacher being assigned to a classroom (as tutor or teaching staff) does not belong to that classroom's
 * school.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@StandardException
public class SchoolMismatchException
        extends RuntimeException {
}
