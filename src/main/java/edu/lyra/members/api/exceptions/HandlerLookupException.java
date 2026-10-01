package edu.lyra.members.api.exceptions;

import lombok.experimental.StandardException;

/**
 * Unchecked adapter around whatever checked exception Spring MVC's handler-mapping lookup declares — in practice
 * only {@code HttpRequestMethodNotSupportedException}, the signal that a path matches a mapping under a different
 * HTTP method than the one requested.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@StandardException
public class HandlerLookupException
        extends RuntimeException {}
