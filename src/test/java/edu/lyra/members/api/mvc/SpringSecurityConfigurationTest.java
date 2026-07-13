package edu.lyra.members.api.mvc;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.ContactInfo;
import edu.lyra.members.api.repositories.jpa.Kid;
import edu.lyra.members.api.repositories.jpa.KidsRepository;
import edu.lyra.members.api.repositories.jpa.Parent;
import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import edu.lyra.members.api.repositories.jpa.School;
import edu.lyra.members.api.repositories.jpa.SchoolsRepository;
import edu.lyra.members.api.repositories.jpa.Teacher;
import edu.lyra.members.api.repositories.jpa.TeachersRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static java.util.UUID.randomUUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SpringSecurityConfigurationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RepositoryRestConfiguration restConfiguration;

    @MockitoSpyBean
    private ParentsRepository parentsRepository;
    @MockitoSpyBean
    private KidsRepository    kidsRepository;
    @MockitoSpyBean
    private SchoolsRepository schoolsRepository;
    @MockitoSpyBean
    private TeachersRepository teachersRepository;

    @Test
    void testCreateParentOk()
            throws Exception {
        final UUID parentId = randomUUID();
        doAnswer(inv -> inv.getArgument(0)).when(parentsRepository).save(any(Parent.class));
        //@formatter:off
        mvc.perform(post(this.base() + "/parents")
                .with(jwt().jwt(b -> b.subject(parentId.toString()))
                           .authorities(new SimpleGrantedAuthority("SCOPE_parents.create")))
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Esteban\",\"surname\":\"Cristóbal\",\"mail\":\"esteban.cristobal@example.com\"}"))
           .andExpect(status().isCreated());
        //@formatter:on
    }

    private String base() {
        return restConfiguration.getBasePath().toString();
    }

    @Test
    void testCreateParentKo()
            throws Exception {
        //@formatter:off
        mvc.perform(post(this.base() + "/parents")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Esteban\",\"surname\":\"Cristóbal\",\"mail\":\"esteban.cristobal@example.com\"}"))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @Test
    void testCreateKidOk()
            throws Exception {
        //@formatter:off
        final UUID parentId = randomUUID();
        final Parent parent = Parent.builder()
                                    .id(parentId)
                                    .contactInfo(ContactInfo.builder()
                                                            .name("Esteban")
                                                            .surname("Cristóbal")
                                                            .mail("esteban.cristobal@example.com")
                                                            .build())
                                    .build();
        //@formatter:on
        doReturn(Optional.of(parent)).when(parentsRepository).findById(parentId);
        doReturn(Instancio.create(Kid.class)).when(kidsRepository).save(any(Kid.class));
        //@formatter:off
        mvc.perform(post(this.base() + "/kids")
                .with(jwt().jwt(b -> b.subject(parentId.toString()))
                           .authorities(new SimpleGrantedAuthority("SCOPE_kids.create")))
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Alicia\",\"surname\":\"Cristóbal\",\"birthdate\":\"2019-12-12\"}"))
           .andExpect(status().isCreated());
        //@formatter:on
        final ArgumentCaptor<Kid> kidCaptor = ArgumentCaptor.forClass(Kid.class);
        verify(kidsRepository).save(kidCaptor.capture());
        assertEquals(parent, kidCaptor.getValue().getParent());
    }

    @Test
    void testCreateKidKo()
            throws Exception {
        //@formatter:off
        mvc.perform(post(this.base() + "/kids")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Alicia\",\"surname\":\"Cristóbal\",\"birthdate\":\"2019-12-12\"}"))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @Test
    void testCreateKidKo_parentNotFound()
            throws Exception {
        doReturn(Optional.empty()).when(parentsRepository).findById(any(UUID.class));
        //@formatter:off
        mvc.perform(post(this.base() + "/kids")
                .with(jwt().jwt(b -> b.subject(randomUUID().toString()))
                           .authorities(new SimpleGrantedAuthority("SCOPE_kids.create")))
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Alicia\",\"surname\":\"Cristóbal\",\"birthdate\":\"2019-12-12\"}"))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @Test
    void testCreateSchoolOk()
            throws Exception {
        doReturn(Instancio.create(School.class)).when(schoolsRepository).save(any(School.class));
        //@formatter:off
        mvc.perform(post(this.base() + "/schools")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_schools.create")))
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Gloria Fuertes\"}"))
           .andExpect(status().isCreated());
        //@formatter:on
    }

    @Test
    void testCreateSchoolKo()
            throws Exception {
        //@formatter:off
        mvc.perform(post(this.base() + "/schools")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Gloria Fuertes\"}"))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @Test
    void testCreateTeacherOk()
            throws Exception {
        final School school = Instancio.create(School.class);
        doReturn(Optional.of(school)).when(schoolsRepository).findById(school.getId());
        doAnswer(inv -> inv.getArgument(0)).when(teachersRepository).save(any(Teacher.class));
        //@formatter:off
        mvc.perform(post(this.base() + "/teachers")
                .with(jwt().jwt(b -> b.subject(randomUUID().toString()))
                           .authorities(new SimpleGrantedAuthority("SCOPE_teachers.create")))
                .contentType(APPLICATION_JSON)
                .content("""
                        {"name":"Marta","surname":"Ibáñez","mail":"marta.ibanez@example.com",\
                        "school":"%s/schools/%s"}""".formatted(this.base(), school.getId())))
           .andExpect(status().isCreated());
        //@formatter:on
    }

    @Test
    void testCreateTeacherKo()
            throws Exception {
        //@formatter:off
        mvc.perform(post(this.base() + "/teachers")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Marta\",\"surname\":\"Ibáñez\",\"mail\":\"marta.ibanez@example.com\"}"))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @Test
    void testActuatorHealthPermitsAllWithoutAuthentication()
            throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void testAnyOtherRequestRequiresAuthentication()
            throws Exception {
        mvc.perform(get(this.base() + "/parents")).andExpect(status().isUnauthorized());
    }

    @Test
    void testAnyOtherRequestIsAllowedWhenAuthenticatedRegardlessOfScope()
            throws Exception {
        //@formatter:off
        mvc.perform(get(this.base() + "/parents")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope"))))
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
