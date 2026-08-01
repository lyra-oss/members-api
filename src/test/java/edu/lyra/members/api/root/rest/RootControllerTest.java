package edu.lyra.members.api.root.rest;

import edu.lyra.members.api.config.web.ApiBasePath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RootControllerTest {

    private RootController controller;

    @BeforeEach
    void setUp() {
        this.controller = new RootController(new ApiBasePath("/v0"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @Test
    void indexLinksToEveryCollection() {
        final RepresentationModel<?> model = this.controller.index();
        for(final String rel : new String[] { "schools", "teachers", "parents", "kids", "classrooms", "persons" }) {
            final String href = model.getRequiredLink(rel).getHref();
            assertTrue(href.endsWith("/v0/" + rel), "Expected link %s to end with /v0/%s".formatted(href, rel));
        }
    }

}
