package edu.lyra.members.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ExceptionRulesTest {

    @ArchTest
    static final ArchRule throwablesLiveInTheExceptionsPackage =
            classes().that().areAssignableTo(Throwable.class)
                     .should().resideInAPackage("..exceptions");

    @ArchTest
    static final ArchRule throwablesAreNamedException =
            classes().that().areAssignableTo(Throwable.class)
                     .should().haveSimpleNameEndingWith("Exception");

}
