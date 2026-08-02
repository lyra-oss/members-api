package edu.lyra.members.api.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.http.ProblemDetail;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class WebRulesTest {

    private static final DescribedPredicate<JavaMethod> ARE_REQUEST_MAPPED =
            DescribedPredicate.describe("are request-mapped",
                                        method -> method.isAnnotatedWith(RequestMapping.class) ||
                                                  method.isAnnotatedWith(GetMapping.class) ||
                                                  method.isAnnotatedWith(PostMapping.class) ||
                                                  method.isAnnotatedWith(PutMapping.class) ||
                                                  method.isAnnotatedWith(PatchMapping.class) ||
                                                  method.isAnnotatedWith(DeleteMapping.class));

    private static final DescribedPredicate<JavaClass> IS_A_CONTROLLER =
            DescribedPredicate.describe("is annotated with @RestController",
                                        javaClass -> javaClass.isAnnotatedWith(RestController.class));

    /**
     * Request-mapped methods ({@code @GetMapping}, {@code @PostMapping}, etc.) declared in a
     * {@code @RestController} must not be public, since Spring MVC always invokes handler methods
     * reflectively rather than through direct calls.
     *
     * <p>Compliant:
     * <pre>{@code
     * @RestController
     * class PersonController {
     *     @GetMapping
     *     Person get(final UUID id) { ... }
     * }
     * }</pre>
     *
     * <p>Violation:
     * <pre>{@code
     * @RestController
     * class PersonController {
     *     @GetMapping
     *     public Person get(final UUID id) { ... }
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule mappedControllerMethodsAreNotPublic =
            methods().that(ARE_REQUEST_MAPPED).and().areDeclaredInClassesThat(IS_A_CONTROLLER)
                     .should().notBePublic();

    /**
     * Controllers own HTTP translation only; they must not depend on a {@link Repository}, so data
     * access always goes through an adapter.
     *
     * <p>Compliant: {@code PersonController} depends on {@code PersonAdapter}
     *
     * <p>Violation: {@code PersonController} depends on {@code PersonRepository} directly
     */
    @ArchTest
    static final ArchRule controllersDoNotDependOnRepositories =
            noClasses().that(IS_A_CONTROLLER).should().dependOnClassesThat().areAssignableTo(Repository.class);

    /**
     * Controllers must not call a method or read/write a field on a JPA {@code @Entity}, so the DTO
     * boundary between the wire format and the persistence model is never crossed. This checks actual
     * access rather than mere type dependency, since a controller legitimately holds a
     * {@code PagedResourcesAssembler<SomeEntity>} field to pass through to its adapter without ever
     * touching the entity itself.
     *
     * <p>Compliant: {@code PersonController} accesses {@code PersonModel}/{@code PersonRequest}
     *
     * <p>Violation: {@code PersonController} calls a method on the {@code Person} entity directly
     */
    @ArchTest
    static final ArchRule controllersDoNotDependOnEntities =
            noClasses().that(IS_A_CONTROLLER).should().accessClassesThat().areAnnotatedWith(Entity.class);

    /**
     * Controllers must not depend on {@code jakarta.persistence..}, reinforcing the DTO boundary at
     * the package level rather than only for {@code @Entity}-annotated types.
     *
     * <p>Compliant: {@code PersonController} has no {@code jakarta.persistence} import
     *
     * <p>Violation: {@code PersonController} imports {@code jakarta.persistence.EntityManager}
     */
    @ArchTest
    static final ArchRule controllersDoNotDependOnPersistence =
            noClasses().that(IS_A_CONTROLLER).should().dependOnClassesThat()
                       .resideInAPackage("jakarta.persistence..");

    /**
     * Controllers must not be {@code @Transactional}; a transaction spans a unit of work owned by the
     * adapter, not the HTTP edge.
     *
     * <p>Compliant: {@code PersonAdapter} (or its repository) is transactional
     *
     * <p>Violation: {@code @Transactional} on a controller class or method
     */
    @ArchTest
    static final ArchRule controllersAreNotTransactional =
            noClasses().that(IS_A_CONTROLLER).should().beAnnotatedWith(Transactional.class);

    /**
     * Controllers must not depend on {@link ProblemDetail}; error shaping stays centralised in
     * {@code ProblemDetailsControllerAdvice} so every error response is built the same way.
     *
     * <p>Compliant: a controller lets an exception propagate to the {@code @ControllerAdvice}
     *
     * <p>Violation: a controller catches an exception and builds a {@code ProblemDetail} itself
     */
    @ArchTest
    static final ArchRule controllersDoNotBuildProblemDetails =
            noClasses().that(IS_A_CONTROLLER).should().dependOnClassesThat().areAssignableTo(ProblemDetail.class);

}
