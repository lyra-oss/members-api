package edu.lyra.members.api.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class JpaRepositoryRulesTest {

    private static final DescribedPredicate<JavaClass> IS_WIRE_OR_MAPPING_LAYER =
            new DescribedPredicate<>("is a Controller, Model, Request or Mapper") {

                @Override
                public boolean test(final JavaClass javaClass) {
                    final String name = javaClass.getSimpleName();
                    return name.endsWith("Controller") || name.endsWith("Model") || name.endsWith("Request") ||
                           name.endsWith("Mapper");
                }
            };

    /**
     * Every Spring Data {@code @Repository} must also be annotated with {@code @Transactional}.
     *
     * <p>Compliant:
     * <pre>{@code
     * @Repository
     * @Transactional
     * interface MemberRepository extends JpaRepository<Member, UUID> { }
     * }</pre>
     *
     * <p>Violation:
     * <pre>{@code
     * @Repository
     * interface MemberRepository extends JpaRepository<Member, UUID> { } // no @Transactional
     * }</pre>
     */
    @ArchTest
    static final ArchRule repositoriesAreTransactional =
            classes().that().areAnnotatedWith(org.springframework.stereotype.Repository.class)
                     .should().beAnnotatedWith(Transactional.class);

    /**
     * Forbids the Jakarta {@code @Transactional} annotation anywhere; use Spring's
     * {@code org.springframework.transaction.annotation.Transactional} instead, since only the Spring
     * annotation is proxy-aware in this codebase.
     *
     * <p>Compliant: {@code import org.springframework.transaction.annotation.Transactional;}
     *
     * <p>Violation: {@code import jakarta.transaction.Transactional;}
     */
    @ArchTest
    static final ArchRule noJakartaTransactional =
            noClasses().should().beAnnotatedWith("jakarta.transaction.Transactional")
                       .as("use org.springframework.transaction.annotation.Transactional, "
                           + "not jakarta.transaction.Transactional");

    /**
     * Controllers, {@code *Model}s, {@code *Request}s and {@code *Mapper}s — the wire format and
     * mapping layer — must never access a {@link Repository} directly; data access is the adapter's
     * (and its collaborators', e.g. a policy or visibility strategy) job.
     *
     * <p>Compliant: {@code SchoolController} calls {@code SchoolAdapter.findById(id)}
     *
     * <p>Violation: {@code SchoolController} calls {@code SchoolRepository.findById(id)} directly
     */
    @ArchTest
    static final ArchRule repositoriesAreNotAccessedByTheWireOrMappingLayer =
            noClasses().that(IS_WIRE_OR_MAPPING_LAYER)
                       .should().accessClassesThat().areAssignableTo(Repository.class);

    /**
     * A repository must not depend on the web layer — neither a "..rest" package nor
     * {@code org.springframework.web..} — since data access must stay usable independently of how (or
     * whether) it is exposed over HTTP.
     *
     * <p>Compliant: {@code SchoolRepository extends JpaRepository<School, UUID>}, no web imports
     *
     * <p>Violation: {@code SchoolRepository} imports something from {@code school.rest} or
     * {@code org.springframework.web}
     */
    @ArchTest
    static final ArchRule repositoriesDoNotDependOnWeb =
            noClasses().that().areAssignableTo(Repository.class).should().dependOnClassesThat()
                       .resideInAnyPackage("..rest..", "org.springframework.web..");

}
