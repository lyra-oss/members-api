package edu.lyra.members.api.kid.rest;

import java.util.List;

import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import org.instancio.Instancio;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminKidVisibilityStrategyTest {

    @Mock
    private KidRepository kidRepository;

    private AdminKidVisibilityStrategy strategy;

    @BeforeEach
    void setUp() {
        this.strategy = new AdminKidVisibilityStrategy(this.kidRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supportsReturnsTrueForAnAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_admin"))));
        assertTrue(this.strategy.supports());
    }

    @Test
    void supportsReturnsFalseForANonAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_parent"))));
        assertFalse(this.strategy.supports());
    }

    @Test
    void findVisibleDelegatesToTheRepositoryForEveryKid() {
        final Pageable  pageable = Pageable.unpaged();
        final Page<Kid> page     = new PageImpl<>(List.of(Instancio.create(Kid.class)));
        when(this.kidRepository.findAll(pageable)).thenReturn(page);
        assertEquals(page, this.strategy.findVisible(pageable));
        verify(this.kidRepository).findAll(pageable);
    }

}
