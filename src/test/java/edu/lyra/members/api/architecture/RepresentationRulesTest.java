package edu.lyra.members.api.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.hateoas.server.core.Relation;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class RepresentationRulesTest {

    private static final DescribedPredicate<JavaClass> IS_A_MODEL =
            DescribedPredicate.describe("has a simple name ending with Model",
                                        javaClass -> javaClass.getSimpleName().endsWith("Model"));

    private static final DescribedPredicate<JavaClass> IS_A_REQUEST =
            DescribedPredicate.describe("has a simple name ending with Request",
                                        javaClass -> javaClass.getSimpleName().endsWith("Request"));

    private static final DescribedPredicate<JavaClass> IS_A_MODEL_OR_REQUEST = IS_A_MODEL.or(IS_A_REQUEST);

    private static final DescribedPredicate<JavaClass> IS_AN_ENTITY_OR_REPOSITORY =
            DescribedPredicate.describe("is annotated with @Entity or assignable to Repository",
                                        javaClass -> javaClass.isAnnotatedWith(Entity.class) ||
                                                     javaClass.isAssignableTo(Repository.class));

    /**
     * Every response {@code *Model} must be annotated {@code @Relation}, declaring its HAL relation name explicitly
     * rather than relying on Spring HATEOAS's simple-name-derived default — a missing annotation silently breaks the
     * collection wrapper name (e.g. {@code _embedded.kidModelList} instead of {@code _embedded.kids}) with no
     * compile-time signal otherwise.
     *
     * <p>Compliant: {@code @Relation(collectionRelation = "kids", itemRelation = "kid") class KidModel}
     *
     * <p>Violation: {@code class KidModel extends RepresentationModel<KidModel>} (no {@code @Relation})
     */
    @ArchTest
    static final ArchRule responseModelsDeclareTheirRelation =
            classes().that(IS_A_MODEL).should().beAnnotatedWith(Relation.class);

    /**
     * A {@code *Request} must not depend on {@code org.springframework.hateoas..}; inbound payloads are plain data,
     * never link-bearing, so only outbound {@code *Model}s use HATEOAS types.
     *
     * <p>Compliant: {@code SchoolRequest} has no HATEOAS import
     *
     * <p>Violation: {@code SchoolRequest} declares a {@code Link} field
     */
    @ArchTest
    static final ArchRule requestDtosDoNotDependOnHateoas =
            noClasses().that(IS_A_REQUEST).should().dependOnClassesThat()
                       .resideInAPackage("org.springframework.hateoas..");

    /**
     * Neither a {@code *Model} nor a {@code *Request} may depend on a JPA {@code @Entity} or a {@link Repository}; the
     * wire format stays fully decoupled from the persistence model, with the {@code *Mapper} as the only bridge between
     * them.
     *
     * <p>Compliant: {@code KidModel} depends only on JDK types and HATEOAS's {@code RepresentationModel}
     *
     * <p>Violation: {@code KidModel} declares a field of type {@code Kid} (the {@code @Entity})
     */
    @ArchTest
    static final ArchRule representationsDoNotDependOnEntitiesOrRepositories =
            //@formatter:off
            noClasses().that(IS_A_MODEL_OR_REQUEST)
                       .should().dependOnClassesThat(IS_AN_ENTITY_OR_REPOSITORY)
                       .as("*Model and *Request must not depend on an @Entity or a Repository");
            //@formatter:on

    /**
     * Neither a {@code *Model} nor a {@code *Request} may depend on {@code jakarta.persistence..}, reinforcing the
     * wire/persistence boundary at the package level rather than only for {@code @Entity}-annotated types.
     *
     * <p>Compliant: no {@code jakarta.persistence} import in {@code KidModel}/{@code KidRequest}
     *
     * <p>Violation: {@code KidRequest} imports {@code jakarta.persistence.Column}
     */
    @ArchTest
    static final ArchRule representationsDoNotDependOnPersistence =
            noClasses().that(IS_A_MODEL_OR_REQUEST).should().dependOnClassesThat()
                       .resideInAPackage("jakarta.persistence..");

}
