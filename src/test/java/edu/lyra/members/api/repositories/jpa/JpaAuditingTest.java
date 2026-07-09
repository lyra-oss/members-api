package edu.lyra.members.api.repositories.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Import(SpringDataJpaConfiguration.class)
class JpaAuditingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ParentsRepository parentsRepository;

    @Autowired
    private SchoolsRepository schoolsRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void populatesCreatedByAndCreatedDateOnInsert() {
        final String subject = this.authenticate();
        //@formatter:off
        final Parent parent = Parent.builder()
                                    .id(UUID.randomUUID())
                                    .name("Esteban")
                                    .surname("Cristóbal")
                                    .mail("esteban.cristobal@example.com")
                                    .build();
        //@formatter:on
        final Parent saved = this.parentsRepository.save(parent);
        this.entityManager.flush();
        assertEquals(subject, saved.getCreatedBy());
        assertEquals(subject, saved.getUpdatedBy());
        assertNotNull(saved.getCreatedDate());
        assertNotNull(saved.getLastModifiedDate());
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
    void keepsCreationAuditInfoAndRefreshesModificationAuditInfoOnUpdate() {
        authenticate();
        //@formatter:off
        final Parent parent = Parent.builder()
                                    .id(UUID.randomUUID())
                                    .name("Esteban")
                                    .surname("Cristóbal")
                                    .mail("esteban.cristobal@example.com")
                                    .build();
        //@formatter:on
        final Parent created = this.parentsRepository.save(parent);
        this.entityManager.flush();
        final String        createdBy   = created.getCreatedBy();
        final LocalDateTime createdDate = created.getCreatedDate();
        final String        editor      = this.authenticate();
        ReflectionTestUtils.setField(created, "surname", "García");
        final Parent updated = this.parentsRepository.save(created);
        this.entityManager.flush();
        assertEquals(createdBy, updated.getCreatedBy());
        assertEquals(createdDate, updated.getCreatedDate());
        assertEquals(editor, updated.getUpdatedBy());
    }

    @Test
    void populatesAuditingFieldsForEveryEntity() {
        final String subject = this.authenticate();
        final School school  = new School();
        ReflectionTestUtils.setField(school, "name", "Gloria Fuertes");
        final School saved = this.schoolsRepository.save(school);
        this.entityManager.flush();
        assertEquals(subject, saved.getCreatedBy());
        assertEquals(subject, saved.getUpdatedBy());
        assertNotNull(saved.getCreatedDate());
        assertNotNull(saved.getLastModifiedDate());
    }

}
