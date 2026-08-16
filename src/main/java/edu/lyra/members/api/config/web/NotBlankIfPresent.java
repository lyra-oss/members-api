package edu.lyra.members.api.config.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

/**
 * Composes {@link Pattern} into the "reject blank, but allow absent" rule every optional {@code String} field on a
 * PATCH request DTO in this codebase needs: {@code null} leaves the field unchanged, but {@code ""} or
 * whitespace-only input is rejected.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = ".*\\S.*", message = "must not be blank")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR,
         ElementType.PARAMETER, ElementType.TYPE_USE})
public @interface NotBlankIfPresent {

    /**
     * @return the validation failure message
     */
    String message() default "must not be blank";

    /**
     * @return the validation groups this constraint belongs to
     */
    Class<?>[] groups() default {};

    /**
     * @return the payload associated with this constraint
     */
    Class<? extends Payload>[] payload() default {};

}
