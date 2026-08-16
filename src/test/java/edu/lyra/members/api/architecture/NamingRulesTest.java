package edu.lyra.members.api.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.mapstruct.Mapper;
import org.springframework.data.repository.Repository;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.conditions.ArchConditions.be;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class NamingRulesTest {

    private static final DescribedPredicate<JavaClass> IS_A_CONTROLLER =
            DescribedPredicate.describe("is annotated with @RestController",
                                        javaClass -> javaClass.isAnnotatedWith(RestController.class));

    private static final DescribedPredicate<JavaClass> IMPLEMENTS_REPRESENTATION_MODEL_ASSEMBLER =
            DescribedPredicate.describe("implements RepresentationModelAssembler",
                                        javaClass -> javaClass.isAssignableTo(RepresentationModelAssembler.class));

    private static final DescribedPredicate<JavaClass> IS_A_MAPSTRUCT_MAPPER =
            DescribedPredicate.describe("is annotated with @Mapper",
                                        javaClass -> javaClass.isAnnotatedWith(Mapper.class));

    private static final DescribedPredicate<JavaClass> EXTENDS_REPRESENTATION_MODEL =
            DescribedPredicate.describe("extends RepresentationModel",
                                        javaClass -> javaClass.isAssignableTo(RepresentationModel.class));

    private static final DescribedPredicate<JavaClass> IS_A_RECORD =
            DescribedPredicate.describe("is a record", JavaClass::isRecord);

    /**
     * Every {@code @RestController} class must have a simple name ending in "Controller".
     *
     * <p>Compliant: {@code @RestController class PersonController}
     *
     * <p>Violation: {@code @RestController class PersonEndpoint}
     */
    @ArchTest
    static final ArchRule controllersAreNamedController =
            classes().that(IS_A_CONTROLLER).should().haveSimpleNameEndingWith("Controller");

    /**
     * The inverse of the rule above: any class named {@code *Controller} must actually carry {@code @RestController},
     * so the name is never misleading.
     *
     * <p>Compliant: {@code @RestController class PersonController}
     *
     * <p>Violation: {@code class PersonController} (missing the annotation)
     */
    @ArchTest
    static final ArchRule namedControllersAreControllers =
            classes().that().haveSimpleNameEndingWith("Controller").should(be(IS_A_CONTROLLER));

    /**
     * Every {@code @RestController} must live in a "..rest" package.
     *
     * <p>Compliant: {@code edu.lyra.members.api.person.rest.PersonController}
     *
     * <p>Violation: {@code edu.lyra.members.api.person.PersonController}
     */
    @ArchTest
    static final ArchRule controllersLiveInRestPackages =
            classes().that(IS_A_CONTROLLER).should().resideInAPackage("..rest");

    /**
     * Every {@link RepresentationModelAssembler} implementation must have a simple name ending in "Adapter".
     *
     * <p>Compliant: {@code class SchoolAdapter implements RepresentationModelAssembler<School, SchoolModel>}
     *
     * <p>Violation: {@code class SchoolAssembler implements RepresentationModelAssembler<School, SchoolModel>}
     */
    @ArchTest
    static final ArchRule assemblersAreNamedAdapter =
            classes().that(IMPLEMENTS_REPRESENTATION_MODEL_ASSEMBLER).should().haveSimpleNameEndingWith("Adapter");

    /**
     * The inverse of the rule above: any class named {@code *Adapter} must actually implement
     * {@link RepresentationModelAssembler}, so the name is never misleading.
     *
     * <p>Compliant: {@code class SchoolAdapter implements RepresentationModelAssembler<School, SchoolModel>}
     *
     * <p>Violation: {@code class SchoolAdapter} (does not implement the interface)
     */
    @ArchTest
    static final ArchRule namedAdaptersAreAssemblers =
            classes().that().haveSimpleNameEndingWith("Adapter").should(be(IMPLEMENTS_REPRESENTATION_MODEL_ASSEMBLER));

    /**
     * Every {@code *Adapter} must live in a "..rest" package.
     *
     * <p>Compliant: {@code edu.lyra.members.api.school.rest.SchoolAdapter}
     *
     * <p>Violation: {@code edu.lyra.members.api.school.SchoolAdapter}
     */
    @ArchTest
    static final ArchRule adaptersLiveInRestPackages =
            classes().that(IMPLEMENTS_REPRESENTATION_MODEL_ASSEMBLER).should().resideInAPackage("..rest");

    /**
     * Every MapStruct {@code @Mapper} must have a simple name ending in "Mapper".
     *
     * <p>Compliant: {@code @Mapper interface SchoolMapper}
     *
     * <p>Violation: {@code @Mapper interface SchoolConverter}
     */
    @ArchTest
    static final ArchRule mapstructMappersAreNamedMapper =
            classes().that(IS_A_MAPSTRUCT_MAPPER).should().haveSimpleNameEndingWith("Mapper");

    /**
     * The inverse of the rule above: any class named {@code *Mapper} must actually carry {@code @Mapper}, so the name
     * is never misleading.
     *
     * <p>Compliant: {@code @Mapper interface SchoolMapper}
     *
     * <p>Violation: {@code interface SchoolMapper} (missing the annotation)
     */
    @ArchTest
    static final ArchRule namedMappersAreMapstructMappers =
            classes().that().haveSimpleNameEndingWith("Mapper").should(be(IS_A_MAPSTRUCT_MAPPER));

    /**
     * Every {@code *Mapper} must live in a "..rest" package.
     *
     * <p>Compliant: {@code edu.lyra.members.api.school.rest.SchoolMapper}
     *
     * <p>Violation: {@code edu.lyra.members.api.school.SchoolMapper}
     */
    @ArchTest
    static final ArchRule mappersLiveInRestPackages =
            classes().that(IS_A_MAPSTRUCT_MAPPER).should().resideInAPackage("..rest");

    /**
     * Every {@link RepresentationModel} subclass must have a simple name ending in "Model".
     *
     * <p>Compliant: {@code class SchoolModel extends RepresentationModel<SchoolModel>}
     *
     * <p>Violation: {@code class SchoolResource extends RepresentationModel<SchoolResource>}
     */
    @ArchTest
    static final ArchRule representationModelsAreNamedModel =
            classes().that(EXTENDS_REPRESENTATION_MODEL).should().haveSimpleNameEndingWith("Model");

    /**
     * The inverse of the rule above: any class named {@code *Model} must actually extend {@link RepresentationModel},
     * so the name is never misleading.
     *
     * <p>Compliant: {@code class SchoolModel extends RepresentationModel<SchoolModel>}
     *
     * <p>Violation: {@code class SchoolModel} (does not extend {@code RepresentationModel})
     */
    @ArchTest
    static final ArchRule namedModelsAreRepresentationModels =
            classes().that().haveSimpleNameEndingWith("Model").should(be(EXTENDS_REPRESENTATION_MODEL));

    /**
     * Every {@code *Model} must live in a "..rest" package.
     *
     * <p>Compliant: {@code edu.lyra.members.api.school.rest.SchoolModel}
     *
     * <p>Violation: {@code edu.lyra.members.api.school.SchoolModel}
     */
    @ArchTest
    static final ArchRule modelsLiveInRestPackages =
            classes().that(EXTENDS_REPRESENTATION_MODEL).should().resideInAPackage("..rest");

    /**
     * Every class named {@code *Request} must be a record, so inbound payloads stay immutable data carriers.
     *
     * <p>Compliant: {@code record SchoolRequest(String name) {}}
     *
     * <p>Violation: {@code class SchoolRequest { private String name; }}
     */
    @ArchTest
    static final ArchRule requestsAreRecords =
            classes().that().haveSimpleNameEndingWith("Request").should(be(IS_A_RECORD));

    /**
     * Every {@code *Request} must live in a "..rest" package.
     *
     * <p>Compliant: {@code edu.lyra.members.api.school.rest.SchoolRequest}
     *
     * <p>Violation: {@code edu.lyra.members.api.school.SchoolRequest}
     */
    @ArchTest
    static final ArchRule requestsLiveInRestPackages =
            classes().that().haveSimpleNameEndingWith("Request").should().resideInAPackage("..rest");

    /**
     * Every class named {@code *Policy} must live in a "..rest" package, alongside the controller and adapter it
     * authorizes for.
     *
     * <p>Compliant: {@code edu.lyra.members.api.school.rest.SchoolPolicy}
     *
     * <p>Violation: {@code edu.lyra.members.api.school.SchoolPolicy}
     */
    @ArchTest
    static final ArchRule policiesLiveInRestPackages =
            classes().that().haveSimpleNameEndingWith("Policy").should().resideInAPackage("..rest");

    /**
     * Every Spring Data {@code Repository} must be declared as an interface (never a class) with a simple name ending
     * in "Repository".
     *
     * <p>Compliant: {@code interface MemberRepository extends JpaRepository<Member, UUID>}
     *
     * <p>Violation: {@code interface MemberDao extends JpaRepository<Member, UUID>}
     */
    @ArchTest
    static final ArchRule springDataRepositoriesAreInterfacesNamedRepository =
            classes().that().areAssignableTo(Repository.class).should().beInterfaces().andShould()
                     .haveSimpleNameEndingWith("Repository");

    /**
     * Spring Data {@code @Repository} beans must live directly in their aggregate's package, not inside its "..rest"
     * sub-package.
     *
     * <p>Compliant: {@code edu.lyra.members.api.person.PersonRepository}
     *
     * <p>Violation: {@code edu.lyra.members.api.person.rest.PersonRepository}
     */
    @ArchTest
    static final ArchRule repositoriesLiveInTheirAggregateRoot =
            classes().that().areAnnotatedWith(org.springframework.stereotype.Repository.class).should()
                     .resideOutsideOfPackages("..rest");

    /**
     * Every {@code @Entity} must also carry an explicit {@code @Table} annotation, so the backing table name is never
     * left to JPA's default naming strategy.
     *
     * <p>Compliant: {@code @Entity @Table(name = "members") class Member}
     *
     * <p>Violation: {@code @Entity class Member} (no {@code @Table})
     */
    @ArchTest
    static final ArchRule entitiesDeclareAnExplicitTable =
            classes().that().areAnnotatedWith(Entity.class).should().beAnnotatedWith(Table.class);

}
