package edu.lyra.members.api.config.web;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers native-image reflection access this application needs from
 * {@code org.hibernate.validator:hibernate-validator}, pulled in by {@code spring-boot-starter-validation} to back
 * every {@code @Valid} request body.
 *
 * <p>{@link #GENERATED_LOGGER_CLASS} and {@link #GENERATED_BUNDLE_CLASS} are generated, at compile time, by JBoss
 * Logging's annotation processor from hibernate-validator's own {@code @MessageLogger}/{@code @MessageBundle}
 * interfaces, and both are looked up by name ({@code MethodHandles.Lookup#findClass}) the first time Bean
 * Validation bootstraps - itself an unconditional step of JPA's {@code EntityManagerFactory} construction, whether
 * or not any entity actually declares a Bean Validation constraint.
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
 * <p>{@link #CONSTRAINT_VALIDATOR_CLASSES}: hibernate-validator's own built-in {@code ConstraintValidator}
 * implementation for every {@code jakarta.validation.constraints} annotation this application's request DTOs
 * declare - one class per constraint-and-target-type combination (e.g. {@code @Size} on a {@code CharSequence} is a
 * different class from {@code @Size} on a {@code Collection}). {@code ConstraintHelper} resolves and instantiates
 * the matching class via {@code Class#getDeclaredConstructor().newInstance()} the first time that constraint is
 * actually validated, not at {@code EntityManagerFactory} construction time - Spring's own Bean Validation AOT
 * support already covers some of these automatically (e.g. {@code NotBlankValidator}, which is why it isn't listed
 * here too), but doesn't reliably resolve constraints with multiple target-type-specific implementations like
 * {@code @Size}; left unregistered, that fails with {@code NoSuchMethodException: <the class>.<init>()}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
class HibernateValidatorRuntimeHints
        implements RuntimeHintsRegistrar {

    private static final String GENERATED_LOGGER_CLASS = "org.hibernate.validator.internal.util.logging.Log_$logger";
    private static final String GENERATED_BUNDLE_CLASS =
            "org.hibernate.validator.internal.util.logging.Messages_$bundle";

    // One entry per constraint-and-target-type combination actually used by a request DTO in this codebase (see
    // edu.lyra.members.api.*.rest.*Request/*PatchRequest and NotBlankIfPresent, which purely composes @Pattern).
    private static final String[] CONSTRAINT_VALIDATOR_CLASSES = {
            // @Size(max = ...) String
            "org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForCharSequence",
            // @Email String
            "org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator",
            // @Pattern(regexp = ...) String, and every @NotBlankIfPresent (a pure @Pattern composition)
            "org.hibernate.validator.internal.constraintvalidators.bv.PatternValidator",
            // @NotNull String
            "org.hibernate.validator.internal.constraintvalidators.bv.NotNullValidator",
            // @Positive Integer/int
            "org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveValidatorForInteger",
            // @Max(...) Integer/int
            "org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MaxValidatorForInteger",
    };

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        hints.reflection().registerTypeIfPresent(
                classLoader, GENERATED_LOGGER_CLASS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        hints.reflection().registerTypeIfPresent(
                classLoader, GENERATED_BUNDLE_CLASS, MemberCategory.ACCESS_PUBLIC_FIELDS);
        for(final String className : CONSTRAINT_VALIDATOR_CLASSES) {
            hints.reflection().registerTypeIfPresent(
                    classLoader, className, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        }
    }

}
