package edu.lyra.members.api.config.web;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers native-image reflection access this application needs from
 * {@code org.hibernate.validator:hibernate-validator}, pulled in by {@code spring-boot-starter-validation} to back
 * every {@code @Valid} request body.
 *
 * <p>{@link #GENERATED_LOGGER_CLASS}: the {@code Log_$logger} class JBoss Logging's annotation processor generates,
 * at compile time, for hibernate-validator's single {@code @MessageLogger} interface. {@code org.jboss.logging.Logger}
 * looks this up by name ({@code MethodHandles.Lookup#findClass}) the first time Bean Validation bootstraps - itself
 * an unconditional step of JPA's {@code EntityManagerFactory} construction, whether or not any entity actually
 * declares a Bean Validation constraint; left unregistered, that fails with {@code IllegalArgumentException: Invalid
 * logger interface org.hibernate.validator.internal.util.logging.Log (implementation not found)}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
class HibernateValidatorRuntimeHints
        implements RuntimeHintsRegistrar {

    private static final String GENERATED_LOGGER_CLASS = "org.hibernate.validator.internal.util.logging.Log_$logger";

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        hints.reflection().registerTypeIfPresent(
                classLoader, GENERATED_LOGGER_CLASS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    }

}
