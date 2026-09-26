package edu.lyra.members.api.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import edu.lyra.members.api.config.security.RequiredAccess;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class SecurityRulesTest {

    private static final String SECURITY_PACKAGE = "..config.security..";

    private static final DescribedPredicate<CanBeAnnotated> IS_HTTP_MAPPED =
            annotatedWith(GetMapping.class).or(annotatedWith(PostMapping.class))
                                            .or(annotatedWith(PutMapping.class))
                                            .or(annotatedWith(PatchMapping.class))
                                            .or(annotatedWith(DeleteMapping.class));

    /**
     * Only classes inside "config.security" may depend on Spring Security's SecurityContextHolder; everywhere else must
     * obtain the current user via AuthenticatedPrincipal instead, keeping security-context access centralized.
     *
     * <p>Compliant: {@code SecurityContextHolder} used from
     * {@code edu.lyra.members.api.config.security.JwtAuthenticatedPrincipalResolver}
     *
     * <p>Violation: {@code SecurityContextHolder} used from
     * {@code edu.lyra.members.api.person.rest.PersonController}
     */
    @ArchTest
    static final ArchRule securityContextIsAccessedOnlyInTheSecurityPackage =
            //@formatter:off
            noClasses().that().resideOutsideOfPackage(SECURITY_PACKAGE)
                       .should().dependOnClassesThat()
                       .haveFullyQualifiedName("org.springframework.security.core.context.SecurityContextHolder")
                       .as("only 'config.security' may touch SecurityContextHolder; "
                           + "everything else should use AuthenticatedPrincipal");
            //@formatter:on

    /**
     * Only classes inside "config.security" may depend on JwtAuthenticationToken; everywhere else must use
     * AuthenticatedPrincipal instead of unwrapping JWTs directly.
     *
     * <p>Compliant: {@code JwtAuthenticationToken} used from
     * {@code edu.lyra.members.api.config.security.JwtAuthenticatedPrincipalResolver}
     *
     * <p>Violation: {@code JwtAuthenticationToken} used from
     * {@code edu.lyra.members.api.person.rest.PersonController}
     */
    @ArchTest
    static final ArchRule jwtAuthenticationIsUnwrappedOnlyInTheSecurityPackage =
            //@formatter:off
            noClasses().that().resideOutsideOfPackage(SECURITY_PACKAGE)
                       .should().dependOnClassesThat()
                       .haveFullyQualifiedName(
                               "org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken")
                       .as("only 'config.security' may unwrap JwtAuthenticationToken; "
                           + "everything else should use AuthenticatedPrincipal");
            //@formatter:on

    /**
     * No method may be annotated with {@code @PreAuthorize}: every access decision is made centrally, before Spring
     * MVC dispatches the request, by {@code RequiredAccessAuthorizationManager} reading the target method's
     * {@code @RequiredAccess} — never inside the controller method itself.
     *
     * <p>{@code @PreAuthorize} runs only after the request body has already been bound and validated, so a caller
     * lacking permission would learn validation details (via a 400) before learning they are unauthorized; the
     * centralized check runs first and always answers with 403 (or 401), closing that gap.
     *
     * <p>Compliant: an access rule expressed as {@code @RequiredAccess(scopes = "parents.create", roles = "admin")}
     * on {@code edu.lyra.members.api.person.rest.PersonController#grantParentRole}
     *
     * <p>Violation: {@code @PreAuthorize("hasRole('admin')")} on any method
     */
    @ArchTest
    static final ArchRule noMethodIsAnnotatedWithPreAuthorize =
            noMethods().should().beAnnotatedWith(PreAuthorize.class)
                       .as("access is granted centrally by RequiredAccessAuthorizationManager reading "
                           + "@RequiredAccess; @PreAuthorize would run after request-body validation and could leak "
                           + "validation errors to a caller who isn't even authorized to see them");

    /**
     * Every HTTP-mapped method in a "..rest" package must carry {@code @RequiredAccess}, so a new endpoint can never
     * go live without an explicit, reviewable access decision.
     *
     * <p>Compliant: {@code @RequiredAccess(scopes = "kids.read")} on
     * {@code edu.lyra.members.api.kid.rest.KidController#findAll}
     *
     * <p>Violation: a {@code @GetMapping}-annotated method in a "..rest" package with no {@code @RequiredAccess}
     */
    @ArchTest
    static final ArchRule everyHttpMappedRestMethodDeclaresRequiredAccess =
            //@formatter:off
            methods().that(IS_HTTP_MAPPED).and().areDeclaredInClassesThat().resideInAPackage("..rest")
                     .should().beAnnotatedWith(RequiredAccess.class)
                     .as("every HTTP-mapped '..rest' method must declare @RequiredAccess, "
                         + "so it is never reachable without an explicit access decision");
            //@formatter:on

}
