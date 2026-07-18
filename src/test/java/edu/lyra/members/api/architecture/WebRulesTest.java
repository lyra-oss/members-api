package edu.lyra.members.api.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class WebRulesTest {

    private static final DescribedPredicate<JavaMethod> ARE_REQUEST_MAPPED =
            new DescribedPredicate<>("are request-mapped") {

                @Override
                public boolean test(final JavaMethod method) {
                    return method.isAnnotatedWith(RequestMapping.class) || method.isAnnotatedWith(GetMapping.class) ||
                           method.isAnnotatedWith(PostMapping.class) || method.isAnnotatedWith(PutMapping.class) ||
                           method.isAnnotatedWith(PatchMapping.class) || method.isAnnotatedWith(DeleteMapping.class);
                }
            };

    private static final DescribedPredicate<JavaMethod> ARE_REPOSITORY_EVENT_HANDLER_METHODS =
            new DescribedPredicate<>("are Spring Data REST @Handle* methods") {

                @Override
                public boolean test(final JavaMethod method) {
                    return method.getAnnotations().stream().anyMatch(
                            annotation -> annotation.getRawType().getName().startsWith(
                                    "org.springframework.data.rest.core.annotation.Handle"));
                }
            };

    /**
     * Forbids {@code @RestController} entirely; controllers must be {@code @RepositoryRestController}
     * instead, so all HTTP endpoints go through Spring Data REST's exposure mechanism.
     */
    @ArchTest
    static final ArchRule noPlainRestControllers =
            noClasses().should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                       .as("controllers should be @RepositoryRestController, not plain @RestController");

    /**
     * Request-mapped methods ({@code @GetMapping}, {@code @PostMapping}, etc.) declared in a
     * {@code @RepositoryRestController} must not be public, since Spring Data REST invokes them
     * reflectively rather than through direct calls.
     */
    @ArchTest
    static final ArchRule mappedControllerMethodsAreNotPublic =
            methods().that(ARE_REQUEST_MAPPED)
                     .and().areDeclaredInClassesThat().areAnnotatedWith(RepositoryRestController.class)
                     .should().notBePublic();

    /**
     * Methods annotated with a Spring Data REST {@code @Handle*} annotation in a
     * {@code @RepositoryEventHandler} must be public, since Spring Data REST needs to invoke them
     * directly.
     */
    @ArchTest
    static final ArchRule handlerMethodsArePublic =
            methods().that(ARE_REPOSITORY_EVENT_HANDLER_METHODS)
                     .and().areDeclaredInClassesThat().areAnnotatedWith(RepositoryEventHandler.class)
                     .should().bePublic();

}
