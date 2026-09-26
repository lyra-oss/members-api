package edu.lyra.members.api.config;

import edu.lyra.members.api.config.jpa.Auditable;
import lombok.experimental.UtilityClass;
import org.instancio.InstancioApi;

import static org.instancio.Select.field;
import static org.instancio.Select.fields;

/**
 * Keeps Instancio's reflective field population off framework-managed fields it isn't supposed to touch, for the
 * entity instances it creates directly (bypassing constructors/setters).
 *
 * @author Esteban Cristóbal Rodríguez
 */
@UtilityClass
public class InstancioSupport {

    /**
     * Ignores {@link Auditable}'s Spring Data JPA-managed fields, and every {@code $$_hibernate_*} field Hibernate's
     * build-time bytecode enhancement (see {@code HibernateBytecodeEnhancer}) injects into every enhanced entity
     * class for lazy-loading/dirty-tracking bookkeeping. Instancio populating those directly - rather than leaving
     * them at their Hibernate-managed defaults - trips {@code EntityEntryContext.addEntityEntry}'s internal
     * consistency assertion the first time such an instance is persisted.
     *
     * @param instancio the in-progress Instancio API to constrain
     * @param <T> the type being created
     * @return {@code instancio} with the framework-managed fields ignored
     */
    public <T> InstancioApi<T> ignoringAuditableFields(final InstancioApi<T> instancio) {
        //@formatter:off
        return instancio.ignore(field(Auditable.class, "version"))
                        .ignore(field(Auditable.class, "createdDate"))
                        .ignore(field(Auditable.class, "createdBy"))
                        .ignore(field(Auditable.class, "lastModifiedDate"))
                        .ignore(field(Auditable.class, "updatedBy"))
                        .ignore(fields(f -> f.getName().startsWith("$$_hibernate_")));
        //@formatter:on
    }

}
