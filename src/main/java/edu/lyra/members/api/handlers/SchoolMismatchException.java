package edu.lyra.members.api.handlers;

public class SchoolMismatchException
        extends RuntimeException {

    public SchoolMismatchException(final String message) {
        super(message);
    }

}
