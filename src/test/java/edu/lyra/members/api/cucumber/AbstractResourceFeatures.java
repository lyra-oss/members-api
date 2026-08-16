package edu.lyra.members.api.cucumber;

import edu.lyra.members.api.config.web.ApiBasePath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.MediaType.APPLICATION_JSON;

public abstract class AbstractResourceFeatures {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ScenarioContext scenarioContext;

    @Autowired
    protected ApiBasePath apiBasePath;

    protected void performWithBody(final MockHttpServletRequestBuilder request, final Object body)
            throws Exception {
        this.perform(request.contentType(APPLICATION_JSON).content(OBJECT_MAPPER.writeValueAsString(body)));
    }

    protected void perform(final MockHttpServletRequestBuilder request)
            throws Exception {
        this.scenarioContext.setResultActions(this.mvc.perform(
                request.with(this.scenarioContext.getJwtProcessor()).contextPath(this.apiBasePath.contextPath())));
    }

}
