package edu.lyra.members.api.config.web;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers native-image reflection access this application needs from
 * {@code org.hibernate.validator:hibernate-validator}, pulled in by {@code spring-boot-starter-validation} to back
 * every {@code @Valid} request body. Both classes below are generated, at compile time, by JBoss Logging's
 * annotation processor from hibernate-validator's own {@code @MessageLogger}/{@code @MessageBundle} interfaces, and
 * both are looked up by name ({@code MethodHandles.Lookup#findClass}) the first time Bean Validation bootstraps -
 * itself an unconditional step of JPA's {@code EntityManagerFactory} construction, whether or not any entity
 * actually declares a Bean Validation constraint.
 *
 * <p>{@link #GENERATED_LOGGER_CLASS}: implements the {@code @MessageLogger} interface;
 * {@code org.jboss.logging.Logger} constructs an instance of it directly. Left unregistered, that fails with
 * {@code IllegalArgumentException: Invalid logger interface org.hibernate.validator.internal.util.logging.Log
 * (implementation not found)}.
 *
 * <p>{@link #GENERATED_BUNDLE_CLASS}: implements the {@code @MessageBundle} interface as a singleton, reached
 * through its own {@code public static final INSTANCE} field rather than construction;
 * {@code org.jboss.logging.Messages} reads that field via {@code MethodHandles.Lookup#findStaticGetter}. Left
 * unregistered, that fails with {@code IllegalArgumentException: Invalid bundle interface
 * org.hibernate.validator.internal.util.logging.Messages (implementation not found)}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
class HibernateValidatorRuntimeHints
        implements RuntimeHintsRegistrar {

    private static final String GENERATED_LOGGER_CLASS = "org.hibernate.validator.internal.util.logging.Log_$logger";
    private static final String GENERATED_BUNDLE_CLASS =
            "org.hibernate.validator.internal.util.logging.Messages_$bundle";

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        hints.reflection().registerTypeIfPresent(
                classLoader, GENERATED_LOGGER_CLASS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        hints.reflection().registerTypeIfPresent(
                classLoader, GENERATED_BUNDLE_CLASS, MemberCategory.ACCESS_PUBLIC_FIELDS);
    }

}
