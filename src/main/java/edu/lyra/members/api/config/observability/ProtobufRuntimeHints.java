package edu.lyra.members.api.config.observability;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers native-image reflection access this application needs from {@code com.google.protobuf:protobuf-java},
 * pulled in transitively by the OTLP metrics exporter ({@code io.opentelemetry}/{@code micrometer-registry-otlp}).
 *
 * <p>{@code ExtensionRegistryFactory} looks up {@code com.google.protobuf.ExtensionRegistry} by name
 * ({@code Class#forName}) and invokes its static factory methods reflectively
 * ({@code Class#getDeclaredMethod(...).invoke(...)}) to decide, at runtime, whether the full (non-lite) protobuf
 * runtime is on the classpath; left unregistered, the first protobuf message class touched fails with
 * {@code MissingReflectionRegistrationError: Cannot reflectively invoke method 'public static
 * com.google.protobuf.ExtensionRegistry com.google.protobuf.ExtensionRegistry.getEmptyRegistry()'}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
class ProtobufRuntimeHints
        implements RuntimeHintsRegistrar {

    private static final String EXTENSION_REGISTRY_CLASS = "com.google.protobuf.ExtensionRegistry";

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        hints.reflection().registerTypeIfPresent(
                classLoader, EXTENSION_REGISTRY_CLASS, MemberCategory.INVOKE_PUBLIC_METHODS);
    }

}
