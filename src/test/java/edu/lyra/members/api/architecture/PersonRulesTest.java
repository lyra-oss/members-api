package edu.lyra.members.api.architecture;

import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import edu.lyra.members.api.person.Person;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class PersonRulesTest {

    private static final String PERSON_PACKAGE = "edu.lyra.members.api.person..";

    private static final String PERSON_BUILDER = "edu.lyra.members.api.person.Person$PersonBuilder";

    private static final Set<String> REGISTRATION_HANDLERS =
            Set.of("edu.lyra.members.api.teacher.rest.TeacherAdapter",
                   "edu.lyra.members.api.parent.rest.ParentAdapter");

    private static final DescribedPredicate<JavaClass> ARE_NOT_ROLE_REGISTRATION_HANDLERS =
            DescribedPredicate.describe("are not the role registration handlers",
                                        javaClass -> ! REGISTRATION_HANDLERS.contains(javaClass.getName()));

    private static final DescribedPredicate<JavaCall<?>> WRITE_TO_A_PERSON =
            DescribedPredicate.describe("write to a Person (constructor, builder or setter)", call -> {
                if(! call.getTargetOwner().isEquivalentTo(Person.class)) {
                    return false;
                }
                final String name = call.getName();
                return name.startsWith("set") || "builder".equals(name) ||
                       JavaConstructor.CONSTRUCTOR_NAME.equals(name);
            });

    /**
     * A {@code Person} may only be written — constructed, built or mutated through a setter — by the person slice
     * itself (which includes {@code PersonRole}'s delegating accessors) or by {@code ParentAdapter}/
     * {@code TeacherAdapter}, which bind a freshly created role to the authenticated subject's identity. Every
     * other class must treat persons as read-only and route identity changes through the role, so the role stays
     * the single write path to a person.
     *
     * <p>Compliant: {@code parent.setName("Ada")} — the role delegates the write to its person
     *
     * <p>Violation: {@code kid.SomeService} calls {@code person.setName("Ada")} or {@code Person.builder()}
     * directly, bypassing the role
     */
    @ArchTest
    static final ArchRule personsAreOnlyMutatedByTheirOwnSliceOrTheRegistrationAdapters =
            //@formatter:off
            noClasses().that().resideOutsideOfPackage(PERSON_PACKAGE)
                       .and(ARE_NOT_ROLE_REGISTRATION_HANDLERS)
                       .should().callCodeUnitWhere(WRITE_TO_A_PERSON)
                       .orShould().dependOnClassesThat().haveFullyQualifiedName(PERSON_BUILDER)
                       .as("a Person may only be created or mutated by the person slice itself or by "
                               + "ParentAdapter/TeacherAdapter; everyone else goes through PersonRole's delegating "
                               + "accessors");
            //@formatter:on

}
