package edu.lyra.members.api.kid.rest;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherKidVisibilityStrategyTest {

    @Mock
    private KidRepository kidRepository;

    private TeacherKidVisibilityStrategy strategy;

    @BeforeEach
    void setUp() {
        this.strategy = new TeacherKidVisibilityStrategy(this.kidRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAsJwt(final UUID subject, final String... authorities) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject.toString()).build();
        final List<SimpleGrantedAuthority> granted =
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, granted));
    }

    @Test
    void supportsReturnsTrueForAnAuthenticatedTeacher() {
        authenticateAsJwt(UUID.randomUUID(), "ROLE_teacher");
        assertTrue(this.strategy.supports());
    }

    @Test
    void supportsReturnsFalseWhenTheRoleDoesNotMatch() {
        authenticateAsJwt(UUID.randomUUID(), "ROLE_parent");
        assertFalse(this.strategy.supports());
    }

    @Test
    void supportsReturnsFalseWhenThereIsNoCurrentId() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_teacher"))));
        assertFalse(this.strategy.supports());
    }

    @Test
    void findVisibleDelegatesToTheRepositoryForTheAuthenticatedTeacher() {
        final UUID       teacherId = UUID.randomUUID();
        final Pageable   pageable  = Pageable.unpaged();
        final Page<Kid>  page      = new PageImpl<>(List.of());
        authenticateAsJwt(teacherId);
        when(this.kidRepository.findByClassroomTaughtOrTutoredBy(teacherId, pageable)).thenReturn(page);
        assertEquals(page, this.strategy.findVisible(pageable));
        verify(this.kidRepository).findByClassroomTaughtOrTutoredBy(teacherId, pageable);
    }

}
