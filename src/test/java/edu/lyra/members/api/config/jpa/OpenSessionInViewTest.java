package edu.lyra.members.api.config.jpa;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins {@code spring.jpa.open-in-view}, which the fetch plan depends on.
 *
 * <p>Every singular association is lazy and each {@code *Adapter} maps entities to models after the repository's
 * transaction has closed, so the persistence context has to survive until the response is rendered. That currently
 * holds because open-session-in-view is on. Turning it off does not fail at startup: it fails at runtime, on the read
 * paths, with {@code LazyInitializationException}. This test makes the dependency explicit and the setting
 * deliberate, rather than inherited from a Spring Boot default that could change.
 *
 * @author Esteban Cristóbal Rodríguez
 */
class OpenSessionInViewTest {

    private static final String PROPERTY = "spring.jpa.open-in-view";

    @Test
    void openSessionInViewIsSetExplicitlyBecauseTheLazyFetchPlanRequiresIt()
            throws IOException {
        final Properties properties = applicationProperties();
        final String     openInView = properties.getProperty(PROPERTY);
        assertNotNull(openInView, PROPERTY + " must be set explicitly, not left to the Spring Boot default");
        assertEquals("true", openInView,
                     "lazy associations are mapped to models outside the repository transaction; "
                     + "turning this off breaks every read path with LazyInitializationException");
    }

    private static Properties applicationProperties()
            throws IOException {
        final Properties properties = new Properties();
        try(InputStream stream = OpenSessionInViewTest.class.getResourceAsStream("/application.properties")) {
            assertNotNull(stream, "application.properties is missing from the classpath");
            properties.load(stream);
        }
        return properties;
    }

}
