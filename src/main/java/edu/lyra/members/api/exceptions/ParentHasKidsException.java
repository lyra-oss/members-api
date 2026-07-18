package edu.lyra.members.api.exceptions;

import lombok.experimental.StandardException;

/**
 * Thrown when a parent is deleted, or has the parent role revoked, while still linked to kids.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@StandardException
public class ParentHasKidsException
        extends RuntimeException {}
