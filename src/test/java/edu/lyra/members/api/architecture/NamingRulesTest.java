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
     * Every @RepositoryRestController class must have a simple name ending in "Controller".
     */
    @ArchTest
    static final ArchRule repositoryRestControllersAreNamedController =
            classes().that().areAnnotatedWith(RepositoryRestController.class)
                     .should().haveSimpleNameEndingWith("Controller");

    /**
     * The inverse of the rule above: any class named *Controller must actually be
     * a @RepositoryRestController, so the name is never misleading.
     */
    @ArchTest
    static final ArchRule controllersAreRepositoryRestControllers =
            classes().that().haveSimpleNameEndingWith("Controller")
                     .should().beAnnotatedWith(RepositoryRestController.class);

    /**
     * Every @RepositoryRestController must live in a "..rest" package.
     */
    @ArchTest
    static final ArchRule controllersLiveInRestPackages =
            classes().that().areAnnotatedWith(RepositoryRestController.class)
                     .should().resideInAPackage("..rest");

    /**
     * Every @RepositoryEventHandler class must have a simple name ending in "Handler".
     */
    @ArchTest
    static final ArchRule repositoryEventHandlersAreNamedHandler =
            classes().that().areAnnotatedWith(RepositoryEventHandler.class)
                     .should().haveSimpleNameEndingWith("Handler");

    /**
     * The inverse of the rule above: any class named *Handler must actually be
     * a @RepositoryEventHandler, so the name is never misleading.
     */
    @ArchTest
    static final ArchRule handlersAreRepositoryEventHandlers =
            classes().that().haveSimpleNameEndingWith("Handler")
                     .should().beAnnotatedWith(RepositoryEventHandler.class);

    /**
     * Every @RepositoryEventHandler must live in a "..handlers" package.
     */
    @ArchTest
    static final ArchRule handlersLiveInHandlersPackages =
            classes().that().areAnnotatedWith(RepositoryEventHandler.class)
                     .should().resideInAPackage("..handlers");

    /**
     * Every Spring Data Repository must be declared as an interface (never a class) with a
     * simple name ending in "Repository".
     */
    @ArchTest
    static final ArchRule springDataRepositoriesAreInterfacesNamedRepository =
            classes().that().areAssignableTo(Repository.class)
                     .should().beInterfaces()
                     .andShould().haveSimpleNameEndingWith("Repository");

    /**
     * Spring Data @Repository beans must live directly in their aggregate's package, not
     * inside its "..rest" or "..handlers" sub-packages.
     */
    @ArchTest
    static final ArchRule repositoriesLiveInTheirAggregateRoot =
            classes().that().areAnnotatedWith(org.springframework.stereotype.Repository.class)
                     .should().resideOutsideOfPackages("..rest", "..handlers");

    /**
     * Every @Entity must also carry an explicit @Table annotation, so the backing
     * table name is never left to JPA's default naming strategy.
     */
    @ArchTest
    static final ArchRule entitiesDeclareAnExplicitTable =
            classes().that().areAnnotatedWith(Entity.class).should().beAnnotatedWith(Table.class);

}
