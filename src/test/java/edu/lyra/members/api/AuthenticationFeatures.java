package edu.lyra.members.api;

import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public class AuthenticationFeatures {

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private ParentsRepository parentsRepository;

    @Given("I am authenticated with {string} scope")
    public void iAmAuthenticatedWithScope(final String scope) {
        scenarioContext.setJwtProcessor(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
                                             .authorities(new SimpleGrantedAuthority("SCOPE_" + scope)));
    }

    @Given("I am authenticated as {string} with {string} scope")
    public void iAmAuthenticatedAsWithScope(final String email, final String scope) {
        final String sub = parentsRepository.findByContactInfoMail(email).orElseThrow().getId().toString();
        scenarioContext.setJwtProcessor(
                jwt().jwt(builder -> builder.subject(sub)).authorities(new SimpleGrantedAuthority("SCOPE_" + scope)));
    }

}
