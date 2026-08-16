package edu.lyra.members.api.config;

import edu.lyra.members.api.config.jpa.Auditable;
import lombok.experimental.UtilityClass;
import org.instancio.InstancioApi;

import static org.instancio.Select.field;

@UtilityClass
public class InstancioSupport {

    public <T> InstancioApi<T> ignoringAuditableFields(final InstancioApi<T> instancio) {
        return instancio.ignore(field(Auditable.class, "version"))
                         .ignore(field(Auditable.class, "createdDate"))
                         .ignore(field(Auditable.class, "createdBy"))
                         .ignore(field(Auditable.class, "lastModifiedDate"))
                         .ignore(field(Auditable.class, "updatedBy"));
    }

}
