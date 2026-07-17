package edu.lyra.members.api.config.security;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.contactinfo.ContactInfo;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidsRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentsRepository;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolsRepository;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeachersRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static java.util.UUID.randomUUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SpringSecurityConfigurationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
                .content(OBJECT_MAPPER.writeValueAsString(this.newParentJson())))
           .andExpect(status().isCreated());
        //@formatter:on
    }

    private String base() {
        return restConfiguration.getBasePath().toString();
    }

    private ObjectNode newParentJson() {
        final ObjectNode parentJson = OBJECT_MAPPER.createObjectNode();
        parentJson.put("name", "Esteban");
        parentJson.put("surname", "Cristóbal");
        parentJson.put("mail", "esteban.cristobal@example.com");
        return parentJson;
    }

    @Test
    void testCreateParentKo()
            throws Exception {
        //@formatter:off
        mvc.perform(post(this.base() + "/parents")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(this.newParentJson())))
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
                .content(OBJECT_MAPPER.writeValueAsString(this.newKidJson())))
           .andExpect(status().isCreated());
        //@formatter:on
        final ArgumentCaptor<Kid> kidCaptor = ArgumentCaptor.forClass(Kid.class);
        verify(kidsRepository).save(kidCaptor.capture());
        assertEquals(parent, kidCaptor.getValue().getParent());
    }

    private ObjectNode newKidJson() {
        final ObjectNode kidJson = OBJECT_MAPPER.createObjectNode();
        kidJson.put("name", "Alicia");
        kidJson.put("surname", "Cristóbal");
        kidJson.put("birthdate", "2019-12-12");
        return kidJson;
    }

    @Test
    void testCreateKidKo()
            throws Exception {
        //@formatter:off
        mvc.perform(post(this.base() + "/kids")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(this.newKidJson())))
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
                .content(OBJECT_MAPPER.writeValueAsString(this.newKidJson())))
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
                .content(OBJECT_MAPPER.writeValueAsString(this.newSchoolJson())))
           .andExpect(status().isCreated());
        //@formatter:on
    }

    private ObjectNode newSchoolJson() {
        final ObjectNode schoolJson = OBJECT_MAPPER.createObjectNode();
        schoolJson.put("name", "Gloria Fuertes");
        return schoolJson;
    }

    @Test
    void testActuatorHealthPermitsAllWithoutAuthentication()
            throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void testCreateSchoolKo()
            throws Exception {
        //@formatter:off
        mvc.perform(post(this.base() + "/schools")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(this.newSchoolJson())))
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
                .content(this.teacherJsonWithSchool(school)))
           .andExpect(status().isCreated());
        //@formatter:on
    }

    private String teacherJsonWithSchool(final School school) {
        final ObjectNode teacherJson = this.newTeacherJson();
        teacherJson.put("school", this.base() + "/schools/" + school.getId());
        return OBJECT_MAPPER.writeValueAsString(teacherJson);
    }

    private ObjectNode newTeacherJson() {
        final ObjectNode teacherJson = OBJECT_MAPPER.createObjectNode();
        teacherJson.put("name", "Marta");
        teacherJson.put("surname", "Ibáñez");
        teacherJson.put("mail", "marta.ibanez@example.com");
        return teacherJson;
    }

    @Test
    void testCreateTeacherKo()
            throws Exception {
        //@formatter:off
        mvc.perform(post(this.base() + "/teachers")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(this.newTeacherJson())))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @ParameterizedTest
    @ValueSource(strings = { "parents", "kids", "schools", "teachers", "classrooms" })
    void testListingRequiresAuthentication(final String resource)
            throws Exception {
        mvc.perform(get(this.base() + "/" + resource)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = { "parents", "kids", "schools", "teachers", "classrooms" })
    void testListingRequiresReadScope(final String resource)
            throws Exception {
        //@formatter:off
        mvc.perform(get(this.base() + "/" + resource)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope"))))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @ParameterizedTest
    @ValueSource(strings = { "parents", "kids", "schools", "teachers", "classrooms" })
    void testListingIsAllowedWithReadScope(final String resource)
            throws Exception {
        //@formatter:off
        mvc.perform(get(this.base() + "/" + resource)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + resource + ".read"))))
           .andExpect(status().isOk());
        //@formatter:on
    }

    @ParameterizedTest
    @ValueSource(strings = { "parents", "kids", "schools", "teachers", "classrooms" })
    void testGettingSingleItemRequiresAuthentication(final String resource)
            throws Exception {
        mvc.perform(get(this.base() + "/" + resource + "/" + randomUUID())).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = { "parents", "kids", "schools", "teachers", "classrooms" })
    void testGettingSingleItemRequiresReadScope(final String resource)
            throws Exception {
        //@formatter:off
        mvc.perform(get(this.base() + "/" + resource + "/" + randomUUID())
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope"))))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @ParameterizedTest
    @ValueSource(strings = { "parents", "kids", "schools", "teachers", "classrooms" })
    void testGettingSingleItemIsAllowedWithReadScope(final String resource)
            throws Exception {
        //@formatter:off
        mvc.perform(get(this.base() + "/" + resource + "/" + randomUUID())
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + resource + ".read"))))
           .andExpect(status().isNotFound());
        //@formatter:on
    }

    @ParameterizedTest
    @ValueSource(strings = { "parents", "kids", "schools", "teachers", "classrooms" })
    void testItemPutIsDisabled(final String resource)
            throws Exception {
        //@formatter:off
        mvc.perform(put(this.base() + "/" + resource + "/" + randomUUID())
                .with(jwt())
                .contentType(APPLICATION_JSON)
                .content("{}"))
           .andExpect(status().isMethodNotAllowed());
        //@formatter:on
    }

    @ParameterizedTest
    @ValueSource(strings = { "parents", "kids", "schools", "teachers", "classrooms" })
    void testItemPatchRequiresAuthentication(final String resource)
            throws Exception {
        //@formatter:off
        mvc.perform(patch(this.base() + "/" + resource + "/" + randomUUID())
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content("{}"))
           .andExpect(status().isUnauthorized());
        //@formatter:on
    }

    @ParameterizedTest
    @ValueSource(strings = { "parents", "kids", "schools", "teachers", "classrooms" })
    void testItemPatchRequiresUpdateScope(final String resource)
            throws Exception {
        //@formatter:off
        mvc.perform(patch(this.base() + "/" + resource + "/" + randomUUID())
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content("{}"))
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
