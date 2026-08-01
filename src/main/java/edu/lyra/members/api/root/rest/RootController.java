package edu.lyra.members.api.root.rest;

import edu.lyra.members.api.config.web.ApiBasePath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
class RootController {

    private static final String[] COLLECTION_RELS =
            { "schools", "teachers", "parents", "kids", "classrooms", "persons" };

    private final ApiBasePath apiBasePath;

    RootController(final ApiBasePath apiBasePath) {
        this.apiBasePath = apiBasePath;
    }

    @GetMapping("${lyra.api.base-path}/")
    RepresentationModel<?> index() {
        log.debug("Building the root link index");
        final RepresentationModel<?> model = new RepresentationModel<>();
        for(final String rel : COLLECTION_RELS) {
            model.add(this.link(rel));
        }
        return model;
    }

    private Link link(final String rel) {
        //@formatter:off
        final String href = ServletUriComponentsBuilder.fromCurrentContextPath()
                                                        .path(this.apiBasePath.basePath())
                                                        .path("/" + rel)
                                                        .toUriString();
        //@formatter:on
        return Link.of(href, rel);
    }

}
