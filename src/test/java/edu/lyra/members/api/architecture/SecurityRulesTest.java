package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.security.access.prepost.PreAuthorize;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class SecurityRulesTest {

    private static final String SECURITY_PACKAGE = "..config.security..";

    /**
     * Only classes inside "config.security" may depend on Spring Security's SecurityContextHolder;
     * everywhere else must obtain the current user via AuthenticatedPrincipal instead, keeping
     * security-context access centralized.
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
     * Only classes inside "config.security" may depend on JwtAuthenticationToken; everywhere else
     * must use AuthenticatedPrincipal instead of unwrapping JWTs directly.
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
     * Every {@code @PreAuthorize}-annotated method must be declared in a "..rest" package, so
     * authorization decisions stay at the HTTP boundary, next to the {@code SecurityFilterChain}
     * matchers they complement.
     *
     * <p>Compliant: {@code @PreAuthorize} on a method of
     * {@code edu.lyra.members.api.person.rest.PersonController}
     *
     * <p>Violation: {@code @PreAuthorize} on a method of
     * {@code edu.lyra.members.api.person.PersonService}
     */
    @ArchTest
    static final ArchRule preAuthorizeOnlyInRestPackages =
            methods().that().areAnnotatedWith(PreAuthorize.class)
                     .should().beDeclaredInClassesThat().resideInAPackage("..rest");

}
