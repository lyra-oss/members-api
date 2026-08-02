package edu.lyra.members.api.root.rest;

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

    @GetMapping("/")
    RepresentationModel<?> index() {
        log.debug("Building the root link index");
        final RepresentationModel<?> model = new RepresentationModel<>();
        for(final String rel : COLLECTION_RELS) {
            model.add(this.link(rel));
        }
        return model;
    }

    // Each *Controller lives in its own vertical slice and is package-private (enforced by
    // VerticalSliceRulesTest), so linkTo(methodOn(...)) - which needs compile-time access to the
    // controller class - isn't available here the way it is inside each slice's own adapter. These are
    // just the collections' well-known, unchanging relative paths, so building them from the current
    // request's already-resolved context path is simpler anyway.
    private Link link(final String rel) {
        final String href = ServletUriComponentsBuilder.fromCurrentContextPath().path("/" + rel).toUriString();
        return Link.of(href, rel);
    }

}
