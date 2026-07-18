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

    @ArchTest
    static final ArchRule repositoryRestControllersAreNamedController =
            classes().that().areAnnotatedWith(RepositoryRestController.class)
                     .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule controllersAreRepositoryRestControllers =
            classes().that().haveSimpleNameEndingWith("Controller")
                     .should().beAnnotatedWith(RepositoryRestController.class);

    @ArchTest
    static final ArchRule controllersLiveInRestPackages =
            classes().that().areAnnotatedWith(RepositoryRestController.class)
                     .should().resideInAPackage("..rest");

    @ArchTest
    static final ArchRule repositoryEventHandlersAreNamedHandler =
            classes().that().areAnnotatedWith(RepositoryEventHandler.class)
                     .should().haveSimpleNameEndingWith("Handler");

    @ArchTest
    static final ArchRule handlersAreRepositoryEventHandlers =
            classes().that().haveSimpleNameEndingWith("Handler")
                     .should().beAnnotatedWith(RepositoryEventHandler.class);

    @ArchTest
    static final ArchRule handlersLiveInHandlersPackages =
            classes().that().areAnnotatedWith(RepositoryEventHandler.class)
                     .should().resideInAPackage("..handlers");

    @ArchTest
    static final ArchRule springDataRepositoriesAreInterfacesNamedRepository =
            classes().that().areAssignableTo(Repository.class)
                     .should().beInterfaces()
                     .andShould().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule repositoriesLiveInTheirAggregateRoot =
            classes().that().areAnnotatedWith(org.springframework.stereotype.Repository.class)
                     .should().resideOutsideOfPackages("..rest", "..handlers");

    @ArchTest
    static final ArchRule entitiesDeclareAnExplicitTable =
            classes().that().areAnnotatedWith(Entity.class).should().beAnnotatedWith(Table.class);

}
