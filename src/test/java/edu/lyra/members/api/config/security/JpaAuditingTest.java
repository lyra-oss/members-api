package edu.lyra.members.api.config.security;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.config.jpa.Auditable;
import edu.lyra.members.api.parent.Parent;
import edu.lyra.members.api.parent.ParentRepository;
import edu.lyra.members.api.person.Person;
import edu.lyra.members.api.school.School;
import edu.lyra.members.api.school.SchoolRepository;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Import(SecurityAuditingConfiguration.class)
class JpaAuditingTest {

    private static final Faker FAKER = new Faker();

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void populatesCreatedByAndCreatedDateOnInsert() {
        final String subject = this.authenticate();
        final Parent parent = Parent.builder().person(aPerson()).build();
        final Parent saved  = this.parentRepository.save(parent);
        this.entityManager.flush();
        assertEquals(subject, saved.getCreatedBy());
        assertEquals(subject, saved.getUpdatedBy());
        assertNotNull(saved.getCreatedDate());
        assertNotNull(saved.getLastModifiedDate());
    }

    private static Person aPerson() {
        //@formatter:off
        return Person.builder()
                     .id(UUID.randomUUID())
                     .name("Esteban")
                     .surname("Cristóbal")
                     .mail("esteban.cristobal@example.com")
                     .build();
        //@formatter:on
    }

    private String authenticate() {
        final String subject = UUID.randomUUID().toString();
        //@formatter:off
        final Jwt jwt = Jwt.withTokenValue("token")
                           .header("alg", "RS256")
                           .subject(subject)
                           .build();
        //@formatter:on
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        return subject;
    }

    @Test
    void keepsCreationAuditInfoAndRefreshesModificationAuditInfoOnPersonUpdate() {
        authenticate();
        final Parent parent  = Parent.builder().person(aPerson()).build();
        final Parent created = this.parentRepository.save(parent);
        this.entityManager.flush();
        final String        createdBy   = created.getPerson().getCreatedBy();
        final LocalDateTime createdDate = created.getPerson().getCreatedDate();
        final String        editor      = this.authenticate();
        created.getPerson().setSurname("García");
        final Parent updated = this.parentRepository.save(created);
        this.entityManager.flush();
        assertEquals(createdBy, updated.getPerson().getCreatedBy());
        assertEquals(createdDate, updated.getPerson().getCreatedDate());
        assertEquals(editor, updated.getPerson().getUpdatedBy());
    }

    @Test
    void populatesAuditingFieldsForEveryEntity() {
        final String subject = this.authenticate();
        final School school =
                Instancio.of(School.class).ignore(field(School.class, "id")).ignore(field(School.class, "classrooms"))
                         .ignore(field(School.class, "teachers")).ignore(field(Auditable.class, "version"))
                         .ignore(field(Auditable.class, "createdDate")).ignore(field(Auditable.class, "createdBy"))
                         .ignore(field(Auditable.class, "lastModifiedDate")).ignore(field(Auditable.class, "updatedBy"))
                         .set(field(School.class, "name"), FAKER.educator().secondarySchool()).create();
        final School saved = this.schoolRepository.save(school);
        this.entityManager.flush();
        assertEquals(subject, saved.getCreatedBy());
        assertEquals(subject, saved.getUpdatedBy());
        assertNotNull(saved.getCreatedDate());
        assertNotNull(saved.getLastModifiedDate());
    }

}
