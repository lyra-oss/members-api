package edu.lyra.members.api.config.jpa;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Guards the two native-image startup crashes {@link HibernateRuntimeHints} exists to prevent: one representative
 * class from each of its two lists, both taken from the actual failures a native build hit before that list entry was
 * added ({@code IllegalArgumentException: Invalid logger interface ... (implementation not found)} for the JBoss
 * Logging classes, {@code NoSuchMethodException: <class>.<init>()} for the default-strategy classes).
 *
 * @author Esteban Cristóbal Rodríguez
 */
class HibernateRuntimeHintsTest {

    private final HibernateRuntimeHints registrar = new HibernateRuntimeHints();
    private final RuntimeHints          hints     = new RuntimeHints();

    @Test
    void registersTheGeneratedJBossLoggerImplementationThatOnceCrashedNativeImageStartup() {
        this.registrar.registerHints(this.hints, getClass().getClassLoader());
        assertRegisteredForConstructorInvocation("org.hibernate.jpa.internal.JpaLogger_$logger");
    }

    @Test
    void registersTheDefaultStrategyImplementationThatOnceCrashedNativeImageStartup() {
        this.registrar.registerHints(this.hints, getClass().getClassLoader());
        assertRegisteredForConstructorInvocation("org.hibernate.boot.model.relational.ColumnOrderingStrategyStandard");
    }

    private void assertRegisteredForConstructorInvocation(final String className) {
        final ReflectionHints reflection = this.hints.reflection();
        final TypeHint        typeHint   = reflection.getTypeHint(TypeReference.of(className));
        assertNotNull(typeHint, className + " should be registered for reflection");
        assertEquals(Set.of(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS), typeHint.getMemberCategories());
    }

}
