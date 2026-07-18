package edu.lyra.members.api.architecture;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import edu.lyra.members.api.config.jpa.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.repository.Repository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "edu.lyra.members.api", importOptions = ImportOption.DoNotIncludeTests.class)
class JpaEntityRulesTest {

    private static final List<Class<? extends Annotation>> AUDITING_FIELD_ANNOTATIONS =
            List.of(Version.class, CreatedDate.class, CreatedBy.class, LastModifiedDate.class, LastModifiedBy.class);

    private static final List<Class<? extends Annotation>> ID_FIELD_ANNOTATIONS =
            List.of(Id.class, JsonIgnore.class, Column.class);

    private static final String MISSING_ENTITY_LISTENERS_MESSAGE =
            "%s is not annotated with @EntityListeners(AuditingEntityListener.class)";

    private static final String MISSING_NO_ARGS_CONSTRUCTOR_MESSAGE =
            "%s does not have a no-args constructor (missing @NoArgsConstructor)";

    private static final String MISSING_LOMBOK_GETTER_MESSAGE =
            "%s does not expose a Lombok-generated getter for field '%s' (missing @Getter)";

    private static final String MISSING_AUDITING_FIELD_MESSAGE =
            "%s does not declare an auditing field annotated with @%s";

    private static final String MISSING_ID_FIELD_MESSAGE =
            "%s does not declare a field annotated with @Id";

    private static final String WRONG_ID_FIELD_TYPE_MESSAGE =
            "%s id field '%s' is not of type UUID";

    private static final String MISSING_ID_FIELD_ANNOTATION_MESSAGE =
            "%s id field '%s' is not annotated with @%s";

    /**
     * Every {@code @Entity} must be annotated with {@code @EntityListeners(AuditingEntityListener.class)},
     * so JPA auditing (created/modified by and date) is actually wired up rather than silently inert.
     *
     * <p>Compliant:
     * <pre>{@code
     * @Entity
     * @EntityListeners(AuditingEntityListener.class)
     * class Member extends Auditable { ... }
     * }</pre>
     *
     * <p>Violation:
     * <pre>{@code
     * @Entity
     * class Member extends Auditable { ... } // no @EntityListeners
     * }</pre>
     */
    @ArchTest
    static final ArchRule jpaEntitiesAreAnnotatedWithEntityListeners =
            //@formatter:off
            classes().that().areAnnotatedWith(Entity.class)
                     .should(new ArchCondition<>("be annotated with @EntityListeners") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             final boolean isAnnotated = javaClass.isAnnotatedWith(EntityListeners.class);
                             events.add(new SimpleConditionEvent(javaClass, isAnnotated,
                                                                 MISSING_ENTITY_LISTENERS_MESSAGE.formatted(
                                                                         javaClass.getFullName())));
                         }
                     });
    //@formatter:on

    /**
     * Every {@code @Entity} must rely on Lombok to generate a no-args constructor ({@code @NoArgsConstructor},
     * which JPA requires) and a getter for every field ({@code @Getter}), instead of hand-written boilerplate
     * that can drift out of sync with the fields.
     *
     * <p>Compliant:
     * <pre>{@code
     * @Entity
     * @Getter
     * @NoArgsConstructor
     * class Member extends Auditable {
     *     private String name;
     * }
     * }</pre>
     *
     * <p>Violation (missing {@code @NoArgsConstructor}):
     * <pre>{@code
     * @Entity
     * @Getter
     * class Member extends Auditable {
     *     private String name;
     *     Member(final String name) { this.name = name; }
     * }
     * }</pre>
     *
     * <p>Violation (missing {@code @Getter}, so field {@code name} has no getter):
     * <pre>{@code
     * @Entity
     * @NoArgsConstructor
     * class Member extends Auditable {
     *     private String name;
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule jpaEntitiesUseLombok =
            //@formatter:off
            classes().that().areAnnotatedWith(Entity.class)
                     .should(new ArchCondition<>(
                             "use Lombok to generate a no-args constructor and a getter for every field") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             final boolean hasNoArgsConstructor = javaClass.getConstructors().stream().anyMatch(
                                     constructor -> constructor.getRawParameterTypes().isEmpty());
                             events.add(new SimpleConditionEvent(javaClass, hasNoArgsConstructor,
                                                                 MISSING_NO_ARGS_CONSTRUCTOR_MESSAGE.formatted(
                                                                         javaClass.getFullName())));
                             final Set<String> methodNames = javaClass.getAllMethods().stream().map(JavaMethod::getName)
                                                                      .collect(Collectors.toSet());
                             javaClass.getAllFields().stream()
                                      .filter(field -> ! field.getModifiers().contains(JavaModifier.STATIC))
                                      .forEach(field -> {
                                          final boolean hasGetter = methodNames.contains(getterNameFor(field));
                                          events.add(new SimpleConditionEvent(javaClass, hasGetter,
                                                                              MISSING_LOMBOK_GETTER_MESSAGE.formatted(
                                                                                      javaClass.getFullName(),
                                                                                      field.getName())));
                                      });
                         }
                     });
    //@formatter:on

    /**
     * Every {@code @Entity} must declare all five standard auditing fields — {@code @Version},
     * {@code @CreatedDate}, {@code @CreatedBy}, {@code @LastModifiedDate} and {@code @LastModifiedBy} —
     * so optimistic locking and audit trails behave consistently across every entity.
     *
     * <p>Compliant:
     * <pre>{@code
     * @Entity
     * class Member extends Auditable {
     *     @Version               private long version;
     *     @CreatedDate           private Instant createdDate;
     *     @CreatedBy             private String createdBy;
     *     @LastModifiedDate      private Instant lastModifiedDate;
     *     @LastModifiedBy        private String lastModifiedBy;
     * }
     * }</pre>
     *
     * <p>Violation ({@code @Version} is missing):
     * <pre>{@code
     * @Entity
     * class Member extends Auditable {
     *     @CreatedDate           private Instant createdDate;
     *     @CreatedBy             private String createdBy;
     *     @LastModifiedDate      private Instant lastModifiedDate;
     *     @LastModifiedBy        private String lastModifiedBy;
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule jpaEntitiesDeclareAllAuditingFields =
            //@formatter:off
            classes().that().areAnnotatedWith(Entity.class)
                     .should(new ArchCondition<>(
                             "declare version, createdDate, createdBy, lastModifiedDate and updatedBy auditing fields") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             AUDITING_FIELD_ANNOTATIONS.forEach(annotation -> {
                                 final boolean hasField = javaClass.getAllFields().stream()
                                                                   .anyMatch(field -> field.isAnnotatedWith(annotation));
                                 events.add(new SimpleConditionEvent(javaClass, hasField,
                                                                     MISSING_AUDITING_FIELD_MESSAGE.formatted(
                                                                             javaClass.getFullName(),
                                                                             annotation.getSimpleName())));
                             });
                         }
                     });
    //@formatter:on

    /**
     * Every {@code @Entity} must declare exactly one identifier field of type {@code UUID}, annotated
     * with {@code @Id}, {@code @JsonIgnore} (so it never leaks into API responses) and {@code @Column},
     * keeping primary keys consistent across the domain model.
     *
     * <p>Compliant:
     * <pre>{@code
     * @Entity
     * class Member extends Auditable {
     *     @Id @JsonIgnore @Column
     *     private UUID id;
     * }
     * }</pre>
     *
     * <p>Violation (wrong type):
     * <pre>{@code
     * @Entity
     * class Member extends Auditable {
     *     @Id @JsonIgnore @Column
     *     private Long id;
     * }
     * }</pre>
     *
     * <p>Violation (missing {@code @JsonIgnore}, so it leaks into API responses):
     * <pre>{@code
     * @Entity
     * class Member extends Auditable {
     *     @Id @Column
     *     private UUID id;
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule jpaEntitiesHaveUuidIdField =
            //@formatter:off
            classes().that().areAnnotatedWith(Entity.class)
                     .should(new ArchCondition<>(
                             "declare a UUID id field annotated with @Id, @JsonIgnore and @Column") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             final Optional<JavaField> idField = javaClass.getAllFields().stream()
                                                                          .filter(field -> field.isAnnotatedWith(
                                                                                  Id.class))
                                                                          .findFirst();
                             if (idField.isEmpty()) {
                                 events.add(new SimpleConditionEvent(javaClass, false,
                                                                     MISSING_ID_FIELD_MESSAGE.formatted(
                                                                             javaClass.getFullName())));
                                 return;
                             }

                             final JavaField field = idField.get();
                             final boolean isUuid = field.getRawType().isEquivalentTo(UUID.class);
                             events.add(new SimpleConditionEvent(javaClass, isUuid,
                                                                 WRONG_ID_FIELD_TYPE_MESSAGE.formatted(
                                                                         javaClass.getFullName(), field.getName())));

                             ID_FIELD_ANNOTATIONS.forEach(annotation -> {
                                 final boolean isAnnotated = field.isAnnotatedWith(annotation);
                                 events.add(new SimpleConditionEvent(javaClass, isAnnotated,
                                                                     MISSING_ID_FIELD_ANNOTATION_MESSAGE.formatted(
                                                                             javaClass.getFullName(), field.getName(),
                                                                             annotation.getSimpleName())));
                             });
                         }
                     });
    //@formatter:on

    /**
     * Every {@code @Entity} must extend the shared {@link Auditable} base class.
     *
     * <p>Compliant: {@code class Member extends Auditable { ... }}
     *
     * <p>Violation: {@code class Member { ... }}
     */
    @ArchTest
    static final ArchRule jpaEntitiesExtendAuditable =
            classes().that().areAnnotatedWith(Entity.class).should().beAssignableTo(Auditable.class);

    /**
     * Entities must stay plain domain objects: they may not depend on Spring Data repositories, on
     * "..rest.." or "..handlers.." classes, or on Spring Security, keeping persistence, web and
     * security concerns out of the domain model.
     *
     * <p>Compliant:
     * <pre>{@code
     * @Entity
     * class Member extends Auditable {
     *     private String name; // only depends on JDK/domain types
     * }
     * }</pre>
     *
     * <p>Violation (depends on a repository):
     * <pre>{@code
     * @Entity
     * class Member extends Auditable {
     *     Member(final MemberRepository repository) { ... }
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule jpaEntitiesDoNotDependOnInfrastructure =
            //@formatter:off
            noClasses().that().areAnnotatedWith(Entity.class)
                       .should().dependOnClassesThat().areAssignableTo(Repository.class)
                       .orShould().dependOnClassesThat().resideInAnyPackage("..rest..", "..handlers..")
                       .orShould().dependOnClassesThat().resideInAPackage("org.springframework.security..")
                       .as("JPA entities should stay free of persistence, web and security infrastructure");
            //@formatter:on

    private static String getterNameFor(final JavaField field) {
        final String name = field.getName();
        return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

}
