package edu.lyra.members.api.buildtools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;

/**
 * Applies Hibernate's build-time bytecode enhancement (lazy-loading, dirty-tracking) to every compiled entity class
 * under a given directory, invoked at {@code process-classes} via {@code exec-maven-plugin} (see pom.xml).
 *
 * <p>Not the dedicated {@code hibernate-enhance-maven-plugin}: its only Maven Central release
 * ({@code org.hibernate.orm.tooling:hibernate-enhance-maven-plugin:7.0.0.Beta1}) generates bytecode incompatible
 * with this project's {@code hibernate-core} version - the plugin is a thin wrapper around
 * {@link org.hibernate.bytecode.enhance.spi.Enhancer}, which lives inside {@code hibernate-core} itself, so this
 * drives that same engine directly at the exact version already on this project's classpath.
 *
 * <p>Every association reached through a lazy {@code @OneToOne}/{@code @ManyToOne} (e.g. {@code PersonRole.person})
 * needs the resulting enhanced class present at compile time: Hibernate's default bytecode provider (ByteBuddy)
 * would otherwise generate it reflectively, the first time it's needed, which a native-image build's closed-world
 * analysis can't accommodate.
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Slf4j
public final class HibernateBytecodeEnhancer {

    private HibernateBytecodeEnhancer() {
    }

    public static void main(final String[] args) throws IOException {
        if(args.length != 1) {
            throw new IllegalArgumentException("Usage: HibernateBytecodeEnhancer <classes-directory>");
        }
        final Path classesDirectory = Paths.get(args[0]);
        final Enhancer enhancer =
                new BytecodeProviderImpl().getEnhancer(new DefaultEnhancementContext());
        final List<Path> classFiles = collectClassFiles(classesDirectory);
        for(final Path classFile : classFiles) {
            final String className = toClassName(classesDirectory, classFile);
            enhancer.discoverTypes(className, Files.readAllBytes(classFile));
        }
        int enhanced = 0;
        for(final Path classFile : classFiles) {
            final String className        = toClassName(classesDirectory, classFile);
            final byte[] originalBytecode  = Files.readAllBytes(classFile);
            final byte[] enhancedBytecode  = enhancer.enhance(className, originalBytecode);
            if(enhancedBytecode != null) {
                Files.write(classFile, enhancedBytecode);
                enhanced++;
            }
        }
        log.info("Hibernate bytecode enhancement: {}/{} classes enhanced under {}", enhanced, classFiles.size(),
                 classesDirectory);
    }

    private static List<Path> collectClassFiles(final Path classesDirectory) throws IOException {
        try(Stream<Path> paths = Files.walk(classesDirectory)) {
            return paths.filter(path -> path.toString().endsWith(".class")).toList();
        }
    }

    private static String toClassName(final Path classesDirectory, final Path classFile) {
        final String relativePath = classesDirectory.relativize(classFile).toString();
        return relativePath.substring(0, relativePath.length() - ".class".length()).replace('/', '.')
                            .replace('\\', '.');
    }

}
