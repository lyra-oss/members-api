package edu.lyra.members.api.exceptions;

import lombok.experimental.StandardException;

/**
 * Thrown when a request references another resource by id (e.g. a teacher's school) and that id does not match any
 * existing record.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@StandardException
public class UnresolvableReferenceException
        extends RuntimeException {}
