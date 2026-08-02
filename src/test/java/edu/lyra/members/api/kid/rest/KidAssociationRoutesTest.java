package edu.lyra.members.api.kid.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.web.ApiBasePath;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.person.PersonRepository;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Replaces KidsAssociationMethodsTest: the same route-shape guarantee (POST/PUT/PATCH on a kid's
// association sub-resources are unsupported; GET works), pinned through HTTP status codes rather than
// by walking Spring Data REST's own metadata, which disappears with the framework (Phase 4.4).
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class KidAssociationRoutesTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApiBasePath apiBasePath;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private KidRepository kidRepository;

    private UUID kidId;

    @BeforeEach
    void setUp() {
        // Auditable.createdBy/updatedBy are populated from SecurityAuditorAware, which needs a
        // JwtAuthenticationToken in the security context - otherwise these direct repository saves
        // fail their NOT NULL constraint.
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(UUID.randomUUID().toString())
                           .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        final UUID   parentId = UUID.randomUUID();
        final Person person =
                Person.builder().id(parentId).name("Esteban").surname("Cristóbal")
                      .mail("esteban.cristobal+" + parentId + "@example.com").build();
        this.personRepository.save(person);
        final Parent parent = Parent.builder().id(parentId).person(person).build();
        this.parentRepository.save(parent);

        final School school = new School();
        school.setName("Gloria Fuertes");
        this.schoolRepository.save(school);

        final Classroom classroom = new Classroom();
        classroom.setCourse(3);
        classroom.setGroup("A");
        classroom.setSchool(school);
        this.classroomRepository.save(classroom);

        final Kid kid = new Kid();
        kid.setName("Alicia");
        kid.setSurname("Cristóbal");
        kid.setBirthdate(LocalDate.of(2019, 12, 12));
        kid.setParent(parent);
        kid.setClassroom(classroom);
        this.kidRepository.save(kid);
        this.kidId = kid.getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String base() {
        return this.apiBasePath.basePath();
    }

    private MockHttpServletRequestBuilder withContextPath(final MockHttpServletRequestBuilder request) {
        return request.contextPath(this.base());
    }

    @ParameterizedTest
    @ValueSource(strings = { "parent", "classroom" })
    void postIsDisabled(final String relation)
            throws Exception {
        this.mvc.perform(this.withContextPath(post(this.base() + "/kids/" + this.kidId + "/" + relation)
                                                       .with(jwt())))
                .andExpect(status().isMethodNotAllowed());
    }

    @ParameterizedTest
    @ValueSource(strings = { "parent", "classroom" })
    void putIsDisabled(final String relation)
            throws Exception {
        this.mvc.perform(this.withContextPath(put(this.base() + "/kids/" + this.kidId + "/" + relation)
                                                       .with(jwt())))
                .andExpect(status().isMethodNotAllowed());
    }

    @ParameterizedTest
    @ValueSource(strings = { "parent", "classroom" })
    void patchIsDisabled(final String relation)
            throws Exception {
        //@formatter:off
        this.mvc.perform(this.withContextPath(patch(this.base() + "/kids/" + this.kidId + "/" + relation)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_kids.update")))))
                .andExpect(status().isMethodNotAllowed());
        //@formatter:on
    }

    @Test
    void getTheKidsParentSucceeds()
            throws Exception {
        //@formatter:off
        this.mvc.perform(this.withContextPath(get(this.base() + "/kids/" + this.kidId + "/parent")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_kids.read"),
                                         new SimpleGrantedAuthority("SCOPE_parents.read")))))
                .andExpect(status().isOk());
        //@formatter:on
    }

    @Test
    void getTheKidsClassroomSucceeds()
            throws Exception {
        //@formatter:off
        this.mvc.perform(this.withContextPath(get(this.base() + "/kids/" + this.kidId + "/classroom")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_kids.read"),
                                         new SimpleGrantedAuthority("SCOPE_classrooms.read")))))
                .andExpect(status().isOk());
        //@formatter:on
    }

    @TestConfiguration
    static class Config {

        @Bean
        JwtDecoder jwtDecoder() {
            return _ -> {
                throw new JwtException("Test JwtDecoder — use jwt() post-processor instead");
            };
        }

    }

}
