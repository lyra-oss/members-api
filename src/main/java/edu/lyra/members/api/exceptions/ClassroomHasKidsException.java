package edu.lyra.members.api.exceptions;

import lombok.experimental.StandardException;

/**
 * Thrown when a classroom is deleted while it still has kids enrolled.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@StandardException
public class ClassroomHasKidsException
        extends RuntimeException {}
