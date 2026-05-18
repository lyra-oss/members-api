package edu.lyra.members.api.controllers;

import java.util.stream.Stream;

import edu.lyra.members.api.repositories.jpa.Kid;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.rest.core.Path;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMapping;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Stream.of;
import static java.util.stream.StreamSupport.stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KidsAssociationMethodsTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ResourceMappings resourceMappings;

    @Autowired
    private MappingContext<?, ?> mappingContext;

    @Autowired
    private RepositoryRestConfiguration restConfiguration;

    @ParameterizedTest
    @MethodSource("kidAssociationEndpoints")
    void testMethods(final MockHttpServletRequestBuilder request, final ResultMatcher expectedStatus)
            throws Exception {
        //@formatter:off
        this.mvc.perform(request)
                .andDo(result -> log.atInfo()
                                              .addArgument(result.getRequest().getMethod())
                                              .addArgument(result.getRequest().getRequestURI())
                                              .addArgument(result.getResponse().getStatus())
                                              .log("Tested {} {} → {}"))
                .andExpect(expectedStatus);
        //@formatter:on
    }

    private Stream<Arguments> kidAssociationEndpoints() {
        return kidAssociationPaths().flatMap(path -> of(arguments(post(path), status().isMethodNotAllowed()),
                                                        arguments(put(path), status().isMethodNotAllowed()),
                                                        arguments(patch(path), status().isMethodNotAllowed()),
                                                        arguments(get(path), status().isNotFound())));
    }

    private Stream<String> kidAssociationPaths() {
        final ResourceMetadata metadata = resourceMappings.getMetadataFor(Kid.class);
        //@formatter:off
        final Path itemPath = new Path("/" + restConfiguration.getBasePath())
                .slash(requireNonNull(metadata).getPath())
                .slash("1");
        return stream(mappingContext.getRequiredPersistentEntity(Kid.class).spliterator(), false)
                .filter(PersistentProperty::isAssociation)
                .map(metadata::getMappingFor)
                .filter(ResourceMapping::isExported)
                .map(mapping -> itemPath.slash(mapping.getPath()).toString());
        //@formatter:on
    }

}
