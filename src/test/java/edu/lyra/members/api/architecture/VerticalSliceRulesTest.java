package edu.lyra.members.api.architecture;

import java.util.Optional;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class VerticalSliceRulesTest {

    private static final String BASE_PACKAGE = "edu.lyra.members.api";

    private static final Set<String> NON_VERTICAL_TOP_LEVEL_PACKAGES = Set.of("config", "exceptions");

    private static final String INTERNAL_PACKAGE_SUFFIX_HANDLERS = ".handlers";
    private static final String INTERNAL_PACKAGE_SUFFIX_REST     = ".rest";

    private static final DescribedPredicate<JavaClass> RESIDES_IN_A_VERTICAL_PACKAGE =
            new DescribedPredicate<>("resides in a vertical (aggregate) package") {

                @Override
                public boolean test(final JavaClass javaClass) {
                    return topLevelPackageOf(javaClass).map(top -> ! NON_VERTICAL_TOP_LEVEL_PACKAGES.contains(top))
                                                       .orElse(false);
                }
            };

    /**
     * Classes in any "..handlers" or "..rest" package must not be public, since they are internal
     * wiring for their vertical slice and should never be referenced directly from other slices.
     */
    @ArchTest
    static final ArchRule handlersAndRestPackagesContainNoPublicClasses =
            noClasses().that().resideInAnyPackage("..handlers", "..rest").should().bePublic();

    /**
     * Classes in a "..handlers" or "..rest" package may only be accessed by other classes within the
     * same aggregate (vertical slice), preventing one feature's internal wiring from leaking into
     * another feature.
     */
    @ArchTest
    static final ArchRule handlersAndRestPackagesAreOnlyAccessedWithinTheirOwnAggregate =
            //@formatter:off
            classes().that().resideInAnyPackage("..handlers", "..rest")
                     .should(new ArchCondition<>("only be accessed by classes within their own aggregate package") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             final String aggregateRoot = aggregateRootPackageOf(javaClass);
                             javaClass.getAccessesToSelf().stream()
                                      .map(JavaAccess::getOriginOwner)
                                      .filter(origin -> ! withinAggregate(origin.getPackageName(), aggregateRoot))
                                      .forEach(origin -> events.add(new SimpleConditionEvent(javaClass, false,
                                              "%s is accessed by %s, which is outside its aggregate package '%s'"
                                                      .formatted(javaClass.getFullName(), origin.getFullName(),
                                                                 aggregateRoot))));
                         }
                     });
    //@formatter:on

    /**
     * Classes in the shared "config" package (the "kernel") must not depend on classes that live in a
     * vertical/aggregate package — except for Spring Data REST's {@code RepositoryRestConfigurer}
     * extension points — keeping shared infrastructure free of feature-specific coupling.
     */
    @ArchTest
    static final ArchRule kernelPackagesDoNotDependOnVerticalPackages =
            //@formatter:off
            noClasses().that().resideInAPackage(BASE_PACKAGE + ".config..")
                       .and().areNotAssignableTo(RepositoryRestConfigurer.class)
                       .should().dependOnClassesThat(RESIDES_IN_A_VERTICAL_PACKAGE);
    //@formatter:on

    private static Optional<String> topLevelPackageOf(final JavaClass javaClass) {
        final String packageName = javaClass.getPackageName();
        final String prefix      = BASE_PACKAGE + ".";
        if(! packageName.startsWith(prefix)) {
            return Optional.empty();
        }
        return Optional.of(packageName.substring(prefix.length()).split("\\.", 2)[0]);
    }

    private static String aggregateRootPackageOf(final JavaClass javaClass) {
        final String packageName = javaClass.getPackageName();
        if(packageName.endsWith(INTERNAL_PACKAGE_SUFFIX_HANDLERS)) {
            return packageName.substring(0, packageName.length() - INTERNAL_PACKAGE_SUFFIX_HANDLERS.length());
        }
        if(packageName.endsWith(INTERNAL_PACKAGE_SUFFIX_REST)) {
            return packageName.substring(0, packageName.length() - INTERNAL_PACKAGE_SUFFIX_REST.length());
        }
        return packageName;
    }

    private static boolean withinAggregate(final String originPackage, final String aggregateRoot) {
        return originPackage.equals(aggregateRoot) || originPackage.startsWith(aggregateRoot + ".");
    }

}
