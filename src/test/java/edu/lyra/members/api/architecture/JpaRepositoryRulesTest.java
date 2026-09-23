package edu.lyra.members.api.architecture;

import java.util.Collection;
import java.util.Locale;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class JpaRepositoryRulesTest {

    private static final String JOIN_FETCH_WITH_PAGEABLE_MESSAGE =
            "%s.%s pages a @Query that uses 'join fetch'; Hibernate cannot paginate that in SQL and silently falls " +
            "back to loading every row and paginating in memory";

    private static final String UNBOUNDED_RESULT_MESSAGE =
            "%s.%s returns %s of entities with no Pageable; return a Page so the result set stays bounded";

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
            classes().that().areAnnotatedWith(org.springframework.stereotype.Repository.class).should()
                     .beAnnotatedWith(Transactional.class);

    /**
     * Forbids the Jakarta {@code @Transactional} annotation anywhere; use Spring's
     * {@code org.springframework.transaction.annotation.Transactional} instead, since only the Spring annotation is
     * proxy-aware in this codebase.
     *
     * <p>Compliant: {@code import org.springframework.transaction.annotation.Transactional;}
     *
     * <p>Violation: {@code import jakarta.transaction.Transactional;}
     */
    @ArchTest
    static final ArchRule noJakartaTransactional =
            noClasses().should().beAnnotatedWith("jakarta.transaction.Transactional")
                       .as("use org.springframework.transaction.annotation.Transactional, " +
                           "not jakarta.transaction.Transactional");

    /**
     * A repository must not depend on the web layer — neither a "..rest" package nor {@code org.springframework.web..}
     * — since data access must stay usable independently of how (or whether) it is exposed over HTTP.
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

    /**
     * A {@code @Query} that uses {@code join fetch} must not also take a {@code Pageable}.
     *
     * <p>A fetch join multiplies rows, so Hibernate cannot apply {@code LIMIT}/{@code OFFSET} in SQL. It does not
     * fail: it warns (HHH000104) and then reads the entire result set into memory to paginate it there. That is the
     * kind of defect that stays invisible until the table is large. Use {@code @EntityGraph} instead, which the
     * singular associations here are fetched with.
     *
     * <p>Compliant:
     * <pre>{@code
     * @EntityGraph(attributePaths = "person")
     * Page<Teacher> findBySchoolId(UUID schoolId, Pageable pageable);
     * }</pre>
     *
     * <p>Violation:
     * <pre>{@code
     * @Query("select t from Teacher t join fetch t.person where t.school.id = :schoolId")
     * Page<Teacher> findBySchoolId(UUID schoolId, Pageable pageable);
     * }</pre>
     */
    @ArchTest
    static final ArchRule pagedQueriesDoNotUseFetchJoins =
            //@formatter:off
            classes().that().areAssignableTo(Repository.class)
                     .should(new ArchCondition<>("never combine a 'join fetch' @Query with a Pageable") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             javaClass.getMethods().stream()
                                      .filter(method -> method.isAnnotatedWith(Query.class))
                                      .filter(JpaRepositoryRulesTest::takesAPageable)
                                      .filter(JpaRepositoryRulesTest::usesAFetchJoin)
                                      .forEach(method -> events.add(new SimpleConditionEvent(javaClass, false,
                                              JOIN_FETCH_WITH_PAGEABLE_MESSAGE.formatted(javaClass.getFullName(),
                                                                                         method.getName()))));
                         }
                     });
    //@formatter:on

    /**
     * A repository may not declare a finder that returns a {@code Collection} of entities without a {@code Pageable}.
     *
     * <p>Every read path in this API is paged, and an unbounded finder is how that guarantee gets lost: the method
     * looks harmless, and then one school has ten thousand kids. Aggregates are reached through paged queries like
     * {@code findByParentIdOrderByNameAsc}, never by returning the lot. Counting is exempt, since a count returns a
     * number rather than rows.
     *
     * <p>Compliant: {@code Page<Kid> findByParentId(UUID parentId, Pageable pageable);}
     *
     * <p>Violation: {@code List<Kid> findByParentId(UUID parentId);}
     */
    @ArchTest
    static final ArchRule repositoryFindersArePaged =
            //@formatter:off
            classes().that().areAssignableTo(Repository.class)
                     .should(new ArchCondition<>("only return collections of entities from paged methods") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             javaClass.getMethods().stream()
                                      .filter(method -> method.getRawReturnType().isAssignableTo(Collection.class))
                                      .filter(method -> ! takesAPageable(method))
                                      .forEach(method -> events.add(new SimpleConditionEvent(javaClass, false,
                                              UNBOUNDED_RESULT_MESSAGE.formatted(
                                                      javaClass.getFullName(), method.getName(),
                                                      method.getRawReturnType().getSimpleName()))));
                         }
                     });
    //@formatter:on

    private static boolean takesAPageable(final JavaMethod method) {
        return method.getRawParameterTypes().stream().anyMatch(type -> type.isAssignableTo(Pageable.class));
    }

    private static boolean usesAFetchJoin(final JavaMethod method) {
        final Query query = method.getAnnotationOfType(Query.class);
        return containsAFetchJoin(query.value()) || containsAFetchJoin(query.countQuery());
    }

    private static boolean containsAFetchJoin(final String jpql) {
        return jpql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").contains("join fetch");
    }

}
