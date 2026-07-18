package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.data.repository.Repository;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.data.rest.webmvc.RepositoryRestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class NamingRulesTest {

    /**
     * Every {@code @RepositoryRestController} class must have a simple name ending in "Controller".
     *
     * <p>Compliant: {@code @RepositoryRestController class PersonController}
     *
     * <p>Violation: {@code @RepositoryRestController class PersonEndpoint}
     */
    @ArchTest
    static final ArchRule repositoryRestControllersAreNamedController =
            classes().that().areAnnotatedWith(RepositoryRestController.class)
                     .should().haveSimpleNameEndingWith("Controller");

    /**
     * The inverse of the rule above: any class named {@code *Controller} must actually be a
     * {@code @RepositoryRestController}, so the name is never misleading.
     *
     * <p>Compliant: {@code @RepositoryRestController class PersonController}
     *
     * <p>Violation: {@code class PersonController} (missing the annotation)
     */
    @ArchTest
    static final ArchRule controllersAreRepositoryRestControllers =
            classes().that().haveSimpleNameEndingWith("Controller")
                     .should().beAnnotatedWith(RepositoryRestController.class);

    /**
     * Every {@code @RepositoryRestController} must live in a "..rest" package.
     *
     * <p>Compliant: {@code edu.lyra.members.api.person.rest.PersonController}
     *
     * <p>Violation: {@code edu.lyra.members.api.person.PersonController}
     */
    @ArchTest
    static final ArchRule controllersLiveInRestPackages =
            classes().that().areAnnotatedWith(RepositoryRestController.class)
                     .should().resideInAPackage("..rest");

    /**
     * Every {@code @RepositoryEventHandler} class must have a simple name ending in "Handler".
     *
     * <p>Compliant: {@code @RepositoryEventHandler class PersonHandler}
     *
     * <p>Violation: {@code @RepositoryEventHandler class PersonEvents}
     */
    @ArchTest
    static final ArchRule repositoryEventHandlersAreNamedHandler =
            classes().that().areAnnotatedWith(RepositoryEventHandler.class)
                     .should().haveSimpleNameEndingWith("Handler");

    /**
     * The inverse of the rule above: any class named {@code *Handler} must actually be a
     * {@code @RepositoryEventHandler}, so the name is never misleading.
     *
     * <p>Compliant: {@code @RepositoryEventHandler class PersonHandler}
     *
     * <p>Violation: {@code class PersonHandler} (missing the annotation)
     */
    @ArchTest
    static final ArchRule handlersAreRepositoryEventHandlers =
            classes().that().haveSimpleNameEndingWith("Handler")
                     .should().beAnnotatedWith(RepositoryEventHandler.class);

    /**
     * Every {@code @RepositoryEventHandler} must live in a "..handlers" package.
     *
     * <p>Compliant: {@code edu.lyra.members.api.person.handlers.PersonHandler}
     *
     * <p>Violation: {@code edu.lyra.members.api.person.PersonHandler}
     */
    @ArchTest
    static final ArchRule handlersLiveInHandlersPackages =
            classes().that().areAnnotatedWith(RepositoryEventHandler.class)
                     .should().resideInAPackage("..handlers");

    /**
     * Every Spring Data {@code Repository} must be declared as an interface (never a class) with a
     * simple name ending in "Repository".
     *
     * <p>Compliant: {@code interface MemberRepository extends JpaRepository<Member, UUID>}
     *
     * <p>Violation: {@code interface MemberDao extends JpaRepository<Member, UUID>}
     */
    @ArchTest
    static final ArchRule springDataRepositoriesAreInterfacesNamedRepository =
            classes().that().areAssignableTo(Repository.class)
                     .should().beInterfaces()
                     .andShould().haveSimpleNameEndingWith("Repository");

    /**
     * Spring Data {@code @Repository} beans must live directly in their aggregate's package, not
     * inside its "..rest" or "..handlers" sub-packages.
     *
     * <p>Compliant: {@code edu.lyra.members.api.person.PersonRepository}
     *
     * <p>Violation: {@code edu.lyra.members.api.person.rest.PersonRepository}
     */
    @ArchTest
    static final ArchRule repositoriesLiveInTheirAggregateRoot =
            classes().that().areAnnotatedWith(org.springframework.stereotype.Repository.class)
                     .should().resideOutsideOfPackages("..rest", "..handlers");

    /**
     * Every {@code @Entity} must also carry an explicit {@code @Table} annotation, so the backing
     * table name is never left to JPA's default naming strategy.
     *
     * <p>Compliant: {@code @Entity @Table(name = "members") class Member}
     *
     * <p>Violation: {@code @Entity class Member} (no {@code @Table})
     */
    @ArchTest
    static final ArchRule entitiesDeclareAnExplicitTable =
            classes().that().areAnnotatedWith(Entity.class).should().beAnnotatedWith(Table.class);

}
