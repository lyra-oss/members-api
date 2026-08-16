package edu.lyra.members.api.config.security;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import edu.lyra.members.api.classroom.ClassroomRepository;
import edu.lyra.members.api.config.web.ApiBasePath;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolRepository;
import edu.lyra.members.api.teacher.Teacher;
import edu.lyra.members.api.teacher.TeacherRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestJwtDecoderConfiguration.class)
class SpringSecurityConfigurationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApiBasePath apiBasePath;

    @MockitoSpyBean
    private ParentRepository    parentRepository;
    @MockitoSpyBean
    private KidRepository       kidRepository;
    @MockitoSpyBean
    private SchoolRepository    schoolRepository;
    @MockitoSpyBean
    private TeacherRepository   teacherRepository;
    @MockitoSpyBean
    private ClassroomRepository classroomRepository;

    @Test
    void testCreateParentOk()
            throws Exception {
        final UUID parentId = randomUUID();
        doAnswer(inv -> {
            final Parent saved = inv.getArgument(0);
            saved.setId(saved.getPerson().getId());
            return saved;
        }).when(parentRepository).save(any(Parent.class));
        //@formatter:off
        this.perform(post(this.base() + "/parents")
                .with(jwt().jwt(b -> b.subject(parentId.toString()))
                           .authorities(new SimpleGrantedAuthority("SCOPE_parents.create")))
                .contentType(APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(this.newParentJson())))
           .andExpect(status().isCreated());
        //@formatter:on
    }

    private ResultActions perform(final MockHttpServletRequestBuilder request)
            throws Exception {
        return mvc.perform(request.contextPath(this.base()));
    }

    private String base() {
        return apiBasePath.contextPath();
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
        this.perform(post(this.base() + "/parents")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(this.newParentJson())))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @Test
    void testCreateKidOk()
            throws Exception {
        final UUID   parentId = randomUUID();
        final Parent parent   = Parent.builder().id(parentId).build();
        doReturn(Optional.of(parent)).when(parentRepository).findById(parentId);
        doReturn(Instancio.create(Kid.class)).when(kidRepository).save(any(Kid.class));
        //@formatter:off
        this.perform(post(this.base() + "/kids")
                .with(jwt().jwt(b -> b.subject(parentId.toString()))
                           .authorities(new SimpleGrantedAuthority("SCOPE_kids.create")))
                .contentType(APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(this.newKidJson())))
           .andExpect(status().isCreated());
        //@formatter:on
        final ArgumentCaptor<Kid> kidCaptor = ArgumentCaptor.forClass(Kid.class);
        verify(kidRepository).save(kidCaptor.capture());
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
        this.perform(post(this.base() + "/kids")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(this.newKidJson())))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @Test
    void testCreateKidKo_parentNotFound()
            throws Exception {
        doReturn(Optional.empty()).when(parentRepository).findById(any(UUID.class));
        //@formatter:off
        this.perform(post(this.base() + "/kids")
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
        doReturn(Instancio.create(School.class)).when(schoolRepository).save(any(School.class));
        //@formatter:off
        this.perform(post(this.base() + "/schools")
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
        this.perform(get(this.base() + "/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void testActuatorInfoPermitsAllWithoutAuthentication()
            throws Exception {
        this.perform(get(this.base() + "/actuator/info")).andExpect(status().isNotFound());
    }

    @Test
    void testActuatorOtherEndpointsRequireAuthentication()
            throws Exception {
        this.perform(get(this.base() + "/actuator/beans")).andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateClassroomOk()
            throws Exception {
        final School school = Instancio.create(School.class);
        doReturn(Optional.of(school)).when(schoolRepository).findById(school.getId());
        doReturn(Instancio.create(Classroom.class)).when(classroomRepository).save(any(Classroom.class));
        //@formatter:off
        this.perform(post(this.base() + "/classrooms")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_classrooms.create")))
                .contentType(APPLICATION_JSON)
                .content(this.classroomJsonWithSchool(school)))
           .andExpect(status().isCreated());
        //@formatter:on
    }

    private String classroomJsonWithSchool(final School school) {
        final ObjectNode classroomJson = OBJECT_MAPPER.createObjectNode();
        classroomJson.put("course", 1);
        classroomJson.put("group", "A");
        classroomJson.put("school", school.getId().toString());
        return OBJECT_MAPPER.writeValueAsString(classroomJson);
    }

    @Test
    void testCreateClassroomKo()
            throws Exception {
        //@formatter:off
        this.perform(post(this.base() + "/classrooms")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content(this.classroomJsonWithSchool(Instancio.create(School.class))))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @Test
    void testCreateSchoolKo()
            throws Exception {
        //@formatter:off
        this.perform(post(this.base() + "/schools")
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
        doReturn(Optional.of(school)).when(schoolRepository).findById(school.getId());
        doAnswer(inv -> {
            final Teacher saved = inv.getArgument(0);
            saved.setId(saved.getPerson().getId());
            return saved;
        }).when(teacherRepository).save(any(Teacher.class));
        //@formatter:off
        this.perform(post(this.base() + "/teachers")
                .with(jwt().jwt(b -> b.subject(randomUUID().toString()))
                           .authorities(new SimpleGrantedAuthority("SCOPE_teachers.create")))
                .contentType(APPLICATION_JSON)
                .content(this.teacherJsonWithSchool(school)))
           .andExpect(status().isCreated());
        //@formatter:on
    }

    private String teacherJsonWithSchool(final School school) {
        final ObjectNode teacherJson = this.newTeacherJson();
        teacherJson.put("school", school.getId().toString());
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
        this.perform(post(this.base() + "/teachers")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(this.newTeacherJson())))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @ParameterizedTest
    @MethodSource("edu.lyra.members.api.config.CrudResourceNames#stream")
    void testListingRequiresAuthentication(final String resource)
            throws Exception {
        this.perform(get(this.base() + "/" + resource)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("edu.lyra.members.api.config.CrudResourceNames#stream")
    void testListingRequiresReadScope(final String resource)
            throws Exception {
        //@formatter:off
        this.perform(get(this.base() + "/" + resource)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope"))))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @ParameterizedTest
    @MethodSource("edu.lyra.members.api.config.CrudResourceNames#stream")
    void testListingIsAllowedWithReadScope(final String resource)
            throws Exception {
        //@formatter:off
        this.perform(get(this.base() + "/" + resource)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + resource + ".read"))))
           .andExpect(status().isOk());
        //@formatter:on
    }

    @ParameterizedTest
    @MethodSource("edu.lyra.members.api.config.CrudResourceNames#stream")
    void testGettingSingleItemRequiresAuthentication(final String resource)
            throws Exception {
        this.perform(get(this.base() + "/" + resource + "/" + randomUUID())).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("edu.lyra.members.api.config.CrudResourceNames#stream")
    void testGettingSingleItemRequiresReadScope(final String resource)
            throws Exception {
        //@formatter:off
        this.perform(get(this.base() + "/" + resource + "/" + randomUUID())
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope"))))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

    @ParameterizedTest
    @MethodSource("edu.lyra.members.api.config.CrudResourceNames#stream")
    void testGettingSingleItemIsAllowedWithReadScope(final String resource)
            throws Exception {
        //@formatter:off
        this.perform(get(this.base() + "/" + resource + "/" + randomUUID())
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_" + resource + ".read"))))
           .andExpect(status().isNotFound());
        //@formatter:on
    }

    @ParameterizedTest
    @MethodSource("edu.lyra.members.api.config.CrudResourceNames#stream")
    void testItemPatchRequiresAuthentication(final String resource)
            throws Exception {
        //@formatter:off
        this.perform(patch(this.base() + "/" + resource + "/" + randomUUID())
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content("{}"))
           .andExpect(status().isUnauthorized());
        //@formatter:on
    }

    @ParameterizedTest
    @MethodSource("edu.lyra.members.api.config.CrudResourceNames#stream")
    void testItemPatchRequiresUpdateScope(final String resource)
            throws Exception {
        //@formatter:off
        this.perform(patch(this.base() + "/" + resource + "/" + randomUUID())
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_other.scope")))
                .contentType(APPLICATION_JSON)
                .content("{}"))
           .andExpect(status().isForbidden());
        //@formatter:on
    }

}
