package edu.lyra.members.api.mvc;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import edu.lyra.members.api.repositories.jpa.Classroom;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;

import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClassroomAssociationExposureTest {

    private static final Set<String> TEACHING_STAFF_PROPERTIES = Set.of("tutor", "teachers");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ResourceMappings resourceMappings;

    @Autowired
    private MappingContext<?, ?> mappingContext;

    @Autowired
    private RepositoryRestConfiguration restConfiguration;

    private Stream<String> nonTeachingStaffAssociationPaths() {
        //@formatter:off
        final ResourceMetadata metadata = this.resourceMappings.getMetadataFor(Classroom.class);
        final Path itemPath = new Path("/" + this.restConfiguration.getBasePath())
                .slash(requireNonNull(metadata).getPath()).slash(UUID.randomUUID().toString());
        return stream(this.mappingContext.getRequiredPersistentEntity(Classroom.class).spliterator(), false)
                .filter(PersistentProperty::isAssociation)
                .filter(property -> ! TEACHING_STAFF_PROPERTIES.contains(property.getName()))
                .map(metadata::getMappingFor).filter(ResourceMapping::isExported)
                .map(mapping -> itemPath.slash(mapping.getPath()).toString());
        //@formatter:on
    }

    @ParameterizedTest
    @MethodSource("nonTeachingStaffAssociationPaths")
    void remainWritableThroughSpringDataRest(final String path)
            throws Exception {
        this.mvc.perform(put(path).with(jwt()))
                .andExpect(result -> assertNotEquals(405, result.getResponse().getStatus()));
    }

    @TestConfiguration
    static class SecurityConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return _ -> {
                throw new JwtException("Test JwtDecoder — use jwt() post-processor instead");
            };
        }

    }

}
