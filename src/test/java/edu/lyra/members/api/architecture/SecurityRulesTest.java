package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Confines all reading of the authenticated principal to the {@code config} infrastructure: vertical slices must go
 * through the {@code AuthenticatedPrincipal} facade rather than reaching into
 * {@link org.springframework.security.core.context.SecurityContextHolder} or {@code JwtAuthenticationToken} directly.
 * This keeps authorization logic from being copy-pasted (and drifting) across handlers and controllers — the whole
 * point of having the facade. The facade itself, the MDC filter and the JPA auditor live under {@code config} and are
 * the only permitted callers. Scoped to main source only, since test code legitimately builds and installs its own
 * security context.
 */
@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class SecurityRulesTest {

    @ArchTest
    static final ArchRule securityContextIsAccessedOnlyThroughTheFacade =
            //@formatter:off
            noClasses().that().resideOutsideOfPackage("..config..")
                       .should().dependOnClassesThat()
                       .haveFullyQualifiedName("org.springframework.security.core.context.SecurityContextHolder")
                       .as("only 'config' may touch SecurityContextHolder; slices should use AuthenticatedPrincipal");
            //@formatter:on

    @ArchTest
    static final ArchRule jwtAuthenticationIsUnwrappedOnlyInConfig =
            //@formatter:off
            noClasses().that().resideOutsideOfPackage("..config..")
                       .should().dependOnClassesThat()
                       .haveFullyQualifiedName(
                               "org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken")
                       .as("only 'config' may unwrap JwtAuthenticationToken; slices should use AuthenticatedPrincipal");
            //@formatter:on

}
