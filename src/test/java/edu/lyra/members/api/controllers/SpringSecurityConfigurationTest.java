package edu.lyra.members.api.controllers;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.Kid;
import edu.lyra.members.api.repositories.jpa.KidsRepository;
import edu.lyra.members.api.repositories.jpa.Parent;
import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import edu.lyra.members.api.repositories.jpa.School;
import edu.lyra.members.api.repositories.jpa.SchoolsRepository;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
                                        .name("Esteban")
                                        .surname("Cristóbal")
                                        .mail("esteban.cristobal@example.com")
                                    .build();
        //@formatter:on
        doReturn(Optional.of(parent)).when(parentsRepository).findById(parentId);
        doAnswer(inv -> inv.getArgument(0)).when(kidsRepository).save(any(Kid.class));
        //@formatter:off
        mvc.perform(post(this.base() + "/kids")
                .with(jwt().jwt(b -> b.subject(parentId.toString()))
                           .authorities(new SimpleGrantedAuthority("SCOPE_kids.create")))
                .contentType(APPLICATION_JSON)
                .content("{\"name\":\"Alicia\",\"surname\":\"Cristóbal\",\"birthdate\":\"2019-12-12\"," +
                         "\"parent\":\"http://localhost" + base() + "/parents/" + parentId + "\"}"))
           .andExpect(status().isCreated());
        //@formatter:on
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
        doAnswer(inv -> inv.getArgument(0)).when(schoolsRepository).save(any(School.class));
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
