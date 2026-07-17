package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Confines all reading of the authenticated principal to {@code config.security}: everything else — vertical slices
 * and even the rest of {@code config} — must go through the {@code AuthenticatedPrincipal} facade rather than reaching
 * into {@link org.springframework.security.core.context.SecurityContextHolder} or {@code JwtAuthenticationToken}
 * directly. This keeps authorization (and auditor) identity resolution in one place instead of copy-pasted (and
 * drifting) across handlers, controllers and configurations. The facade, the MDC filter and the security-sourced JPA
 * auditor are the only permitted callers, and they all live here. Scoped to main source only, since test code
 * legitimately builds and installs its own security context.
 */
@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class SecurityRulesTest {

    private static final String SECURITY_PACKAGE = "..config.security..";

    @ArchTest
    static final ArchRule securityContextIsAccessedOnlyInTheSecurityPackage =
            //@formatter:off
            noClasses().that().resideOutsideOfPackage(SECURITY_PACKAGE)
                       .should().dependOnClassesThat()
                       .haveFullyQualifiedName("org.springframework.security.core.context.SecurityContextHolder")
                       .as("only 'config.security' may touch SecurityContextHolder; "
                           + "everything else should use AuthenticatedPrincipal");
            //@formatter:on

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

}
