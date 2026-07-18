package edu.lyra.members.api.exceptions;

import lombok.experimental.StandardException;

/**
 * Thrown when a school is deleted while it still has classrooms or teachers linked to it.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@StandardException
public class SchoolHasReferencesException
        extends RuntimeException {}
