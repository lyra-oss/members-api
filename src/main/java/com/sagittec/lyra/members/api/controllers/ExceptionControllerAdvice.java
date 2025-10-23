package com.sagittec.lyra.members.api.controllers;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.CONFLICT;

@ControllerAdvice
class ExceptionControllerAdvice {

    @ResponseStatus(CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    void handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        // Empty method
    }

}
