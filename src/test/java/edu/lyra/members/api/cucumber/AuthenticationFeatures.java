package edu.lyra.members.api.cucumber;

import java.util.UUID;

import edu.lyra.members.api.person.PersonRepository;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public class AuthenticationFeatures {

    @Autowired
    private ScenarioContext scenarioContext;

    @Autowired
    private PersonRepository personRepository;

    @Given("I am authenticated with {string} scope")
    public void iAmAuthenticatedWithScope(final String scope) {
        scenarioContext.setJwtProcessor(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
                                             .authorities(new SimpleGrantedAuthority("SCOPE_" + scope)));
    }

    @Given("I am authenticated as {string} with {string} scope")
    public void iAmAuthenticatedAsWithScope(final String email, final String scope) {
        final String sub = personRepository.findByMail(email).orElseThrow().getId().toString();
        scenarioContext.setJwtProcessor(jwt().jwt(builder -> builder.subject(sub))
                                             .authorities(new SimpleGrantedAuthority("SCOPE_" + scope),
                                                          new SimpleGrantedAuthority("ROLE_parent")));
    }

    @Given("I am authenticated as teacher {string} with {string} scope")
    public void iAmAuthenticatedAsTeacherWithScope(final String teacherName, final String scope) {
        final String location = scenarioContext.getLocation("teacher:" + teacherName);
        final String sub      = location.substring(location.lastIndexOf('/') + 1);
        scenarioContext.setJwtProcessor(jwt().jwt(builder -> builder.subject(sub))
                                             .authorities(new SimpleGrantedAuthority("SCOPE_" + scope),
                                                          new SimpleGrantedAuthority("ROLE_teacher")));
    }

    @Given("I am authenticated as an admin with {string} scope")
    public void iAmAuthenticatedAsAdminWithScope(final String scope) {
        scenarioContext.setJwtProcessor(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
                                             .authorities(new SimpleGrantedAuthority("SCOPE_" + scope),
                                                          new SimpleGrantedAuthority("ROLE_admin")));
    }

}
