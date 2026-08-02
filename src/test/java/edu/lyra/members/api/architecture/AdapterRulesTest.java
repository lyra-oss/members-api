package edu.lyra.members.api.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import org.mapstruct.Mapper;
import org.springframework.data.repository.Repository;
import org.springframework.security.access.AccessDeniedException;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class AdapterRulesTest {

    private static final DescribedPredicate<JavaClass> IS_A_MAPSTRUCT_MAPPER =
            DescribedPredicate.describe("is annotated with @Mapper",
                                        javaClass -> javaClass.isAnnotatedWith(Mapper.class));

    private static final DescribedPredicate<JavaClass> IS_A_MAPPER_PURITY_VIOLATION =
            DescribedPredicate.describe(
                    "a Repository, AuthenticatedPrincipal, or org.springframework.security.. type",
                    javaClass -> javaClass.isAssignableTo(Repository.class) ||
                                 javaClass.getFullName().equals(AuthenticatedPrincipal.class.getName()) ||
                                 javaClass.getPackageName().startsWith("org.springframework.security"));

    private static final DescribedPredicate<JavaClass> MAY_THROW_ACCESS_DENIED =
            DescribedPredicate.<JavaClass>describe("has a simple name ending with Policy",
                                                    javaClass -> javaClass.getSimpleName().endsWith("Policy"))
                               .or(resideInAPackage("..config.security.."));

    /**
     * A MapStruct {@code @Mapper} must stay a pure conversion function: it must not depend on a
     * {@link Repository}, {@link AuthenticatedPrincipal}, or anything from
     * {@code org.springframework.security..}. Mapping is a data transformation, never a place to make
     * an authorization or data-access decision.
     *
     * <p>Compliant: {@code SchoolMapper} depends only on {@code School}/{@code SchoolRequest}/
     * {@code SchoolModel}
     *
     * <p>Violation: a {@code @Mapper} interface calls {@code AuthenticatedPrincipal.isAdmin()}
     */
    @ArchTest
    static final ArchRule mappersArePure =
            //@formatter:off
            noClasses().that(IS_A_MAPSTRUCT_MAPPER)
                       .should().dependOnClassesThat(IS_A_MAPPER_PURITY_VIOLATION)
                       .as("*Mapper must stay pure: no Repository, AuthenticatedPrincipal, "
                           + "or org.springframework.security dependency");
            //@formatter:on

    /**
     * A MapStruct {@code @Mapper} must not declare {@code componentModel = "spring"}; mappers are
     * plain generated classes wired manually via {@code Mappers.getMapper(...)} inside a
     * {@code @Configuration} class. This is belt-and-braces alongside {@code SpringBeanRulesTest},
     * which already forbids component-scanned beans outside "config".
     *
     * <p>Compliant: {@code @Mapper interface SchoolMapper}
     *
     * <p>Violation: {@code @Mapper(componentModel = "spring") interface SchoolMapper}
     */
    @ArchTest
    static final ArchRule mappersDoNotUseSpringComponentModel =
            //@formatter:off
            classes().that(IS_A_MAPSTRUCT_MAPPER)
                     .should(new ArchCondition<JavaClass>("not declare @Mapper(componentModel = \"spring\")") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             final String componentModel =
                                     javaClass.getAnnotationOfType(Mapper.class).componentModel();
                             final boolean satisfied = ! "spring".equals(componentModel);
                             events.add(new SimpleConditionEvent(javaClass, satisfied,
                                     "%s declares @Mapper(componentModel = \"%s\")".formatted(
                                             javaClass.getFullName(), componentModel)));
                         }
                     });
            //@formatter:on

    /**
     * {@link AccessDeniedException} must be thrown only from a {@code *Policy} class, or from the
     * security kernel in "config.security" ({@link AuthenticatedPrincipal} itself, which requires a
     * valid authenticated subject before anything else runs) — one place owns the 403 decision.
     *
     * <p>Compliant: {@code SchoolPolicy} throws {@code AccessDeniedException}
     *
     * <p>Violation: {@code SchoolAdapter} throws {@code AccessDeniedException} directly
     */
    @ArchTest
    static final ArchRule onlyPoliciesThrowAccessDenied =
            //@formatter:off
            noClasses().that(DescribedPredicate.not(MAY_THROW_ACCESS_DENIED))
                       .should().dependOnClassesThat().areAssignableTo(AccessDeniedException.class)
                       .as("AccessDeniedException should be thrown only from a *Policy class "
                           + "(or the security kernel in config.security); one place owns 403");
            //@formatter:on

}
