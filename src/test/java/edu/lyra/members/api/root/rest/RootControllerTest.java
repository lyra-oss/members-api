package edu.lyra.members.api.root.rest;

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
        this.controller = new RootController();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @Test
    void indexLinksToEveryCollection() {
        final RepresentationModel<?> model = this.controller.index();
        for(final String rel : new String[] { "schools", "teachers", "parents", "kids", "classrooms", "persons" }) {
            final String href = model.getRequiredLink(rel).getHref();
            assertTrue(href.endsWith("/" + rel), "Expected link %s to end with /%s".formatted(href, rel));
        }
    }

}
