package edu.lyra.members.api.architecture;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
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
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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

    private static final List<Class<? extends Annotation>> ID_FIELD_ANNOTATIONS = List.of(Id.class, Column.class);

    private static final String HIBERNATE_ENHANCED_FIELD_PREFIX = "$$_hibernate_";

    private static final List<Class<? extends Annotation>> ASSOCIATION_ANNOTATIONS =
            List.of(ManyToOne.class, OneToOne.class, OneToMany.class, ManyToMany.class);

    private static final String MISSING_ENTITY_LISTENERS_MESSAGE =
            "%s is not annotated with @EntityListeners(AuditingEntityListener.class)";

    private static final String MISSING_NO_ARGS_CONSTRUCTOR_MESSAGE =
            "%s does not have a no-args constructor (missing @NoArgsConstructor)";

    private static final String MISSING_LOMBOK_GETTER_MESSAGE =
            "%s does not expose a Lombok-generated getter for field '%s' (missing @Getter)";

    private static final String MISSING_AUDITING_FIELD_MESSAGE =
            "%s does not declare an auditing field annotated with @%s";

    private static final String MISSING_ID_FIELD_MESSAGE = "%s does not declare a field annotated with @Id";

    private static final String WRONG_ID_FIELD_TYPE_MESSAGE = "%s id field '%s' is not of type UUID";

    private static final String MISSING_ID_FIELD_ANNOTATION_MESSAGE = "%s id field '%s' is not annotated with @%s";

    private static final String EAGER_ASSOCIATION_MESSAGE =
            "%s association '%s' is fetched EAGERly; declare fetch = FetchType.LAZY explicitly";

    private static final String ONE_TO_MANY_WITH_JOIN_COLUMN_MESSAGE =
            "%s collection '%s' maps @OneToMany with @JoinColumn; use mappedBy so the column has a single owner";

    private static final String TO_STRING_TOUCHES_ASSOCIATION_MESSAGE =
            "%s toString() reads association '%s'; exclude it (@ToString.Exclude) so printing cannot trigger a lazy " +
            "load";

    /**
     * Every {@code @Entity} must be annotated with {@code @EntityListeners(AuditingEntityListener.class)}, so JPA
     * auditing (created/modified by and date) is actually wired up rather than silently inert.
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
     * Every {@code @Entity} must rely on Lombok to generate a no-args constructor ({@code @NoArgsConstructor}, which
     * JPA requires) and a getter for every field ({@code @Getter}), instead of hand-written boilerplate that can drift
     * out of sync with the fields.
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
                             // $$_hibernate_* fields are injected by build-time bytecode enhancement (see
                             // HibernateBytecodeEnhancer) into every entity class for lazy-loading/dirty-tracking
                             // bookkeeping - framework plumbing, not domain data, so exempt from the getter rule.
                             javaClass.getAllFields().stream()
                                      .filter(field -> ! field.getModifiers().contains(JavaModifier.STATIC))
                                      .filter(field -> ! field.getName().startsWith(HIBERNATE_ENHANCED_FIELD_PREFIX))
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
     * Every {@code @Entity} must declare all five standard auditing fields — {@code @Version}, {@code @CreatedDate},
     * {@code @CreatedBy}, {@code @LastModifiedDate} and {@code @LastModifiedBy} — so optimistic locking and audit
     * trails behave consistently across every entity.
     *
     * <p>Compliant:
     * <pre>{@code
     * @Entity
     * class Member extends Auditable {
     *     @Version private long version;
     *     @CreatedDate private Instant createdDate;
     *     @CreatedBy private String createdBy;
     *     @LastModifiedDate private Instant lastModifiedDate;
     *     @LastModifiedBy private String lastModifiedBy;
     * }
     * }</pre>
     *
     * <p>Violation ({@code @Version} is missing):
     * <pre>{@code
     * @Entity
     * class Member extends Auditable {
     *     @CreatedDate private Instant createdDate;
     *     @CreatedBy private String createdBy;
     *     @LastModifiedDate private Instant lastModifiedDate;
     *     @LastModifiedBy private String lastModifiedBy;
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
     * Every {@code @Entity} must declare exactly one identifier field of type {@code UUID}, annotated with {@code @Id}
     * and {@code @Column}, keeping primary keys consistent across the domain model. Entities are never serialized
     * directly (every response goes through a {@code *Model}), so there is no {@code @JsonIgnore} requirement here —
     * see {@link #jpaEntitiesCarryNoJacksonAnnotations}.
     *
     * <p>Compliant:
     * <pre>{@code
     * @Entity
     * class Member extends Auditable {
     *     @Id @Column
     *     private UUID id;
     * }
     * }</pre>
     *
     * <p>Violation (wrong type):
     * <pre>{@code
     * @Entity
     * class Member extends Auditable {
     *     @Id @Column
     *     private Long id;
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule jpaEntitiesHaveUuidIdField =
            //@formatter:off
            classes().that().areAnnotatedWith(Entity.class)
                     .should(new ArchCondition<>(
                             "declare a UUID id field annotated with @Id and @Column") {

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
     * Entities must stay plain domain objects: they may not depend on Spring Data repositories, on "..rest.." classes,
     * on Spring Security, or on Spring HATEOAS, keeping persistence, web and security concerns out of the domain
     * model.
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
                       .orShould().dependOnClassesThat().resideInAnyPackage("..rest..")
                       .orShould().dependOnClassesThat().resideInAPackage("org.springframework.security..")
                       .orShould().dependOnClassesThat().resideInAPackage("org.springframework.hateoas..")
                       .as("JPA entities should stay free of persistence, web and security infrastructure");
            //@formatter:on

    /**
     * No {@code @Entity} may carry a Jackson annotation ({@code com.fasterxml.jackson..} or {@code tools.jackson..});
     * entities are never serialized directly, so wire-format concerns belong on the {@code *Model} that represents them
     * instead.
     *
     * <p>Compliant: {@code @Entity class Member extends Auditable { private String name; }}
     *
     * <p>Violation: {@code @Entity class Member extends Auditable { @JsonIgnore private UUID id; }}
     */
    @ArchTest
    static final ArchRule jpaEntitiesCarryNoJacksonAnnotations =
            //@formatter:off
            noClasses().that().areAnnotatedWith(Entity.class)
                       .should().beAnnotatedWith(DescribedPredicate.describe(
                               "an annotation from com.fasterxml.jackson.. or tools.jackson..",
                               annotation -> annotation.getRawType().getPackageName().startsWith(
                                       "com.fasterxml.jackson") ||
                                             annotation.getRawType().getPackageName().startsWith("tools.jackson")))
                       .as("JPA entities should carry no Jackson annotations; the *Model owns the wire format");
            //@formatter:on

    /**
     * No {@code @Entity} may carry a Bean Validation annotation ({@code jakarta.validation..}); validation happens on
     * the request DTOs at the API boundary instead.
     *
     * <p>Compliant: {@code @Entity class Member extends Auditable { private String name; }}
     *
     * <p>Violation: {@code @Entity class Member extends Auditable { @NotBlank private String name; }}
     */
    @ArchTest
    static final ArchRule jpaEntitiesCarryNoBeanValidationAnnotations =
            //@formatter:off
            noClasses().that().areAnnotatedWith(Entity.class)
                       .should().beAnnotatedWith(DescribedPredicate.describe(
                               "an annotation from jakarta.validation..",
                               annotation -> annotation.getRawType().getPackageName().startsWith(
                                       "jakarta.validation")))
                       .as("JPA entities should carry no Bean Validation annotations; validation lives on request "
                               + "DTOs");
            //@formatter:on

    /**
     * Every singular association ({@code @ManyToOne}, {@code @OneToOne}) must declare {@code fetch = FetchType.LAZY}.
     *
     * <p>JPA defaults these to {@code EAGER}, which is almost never what a paged read path wants: Hibernate walks the
     * whole object graph for every row and the {@code *Model} then discards it. A page of kids over twenty distinct
     * parents cost forty-two statements before these associations were made lazy; it costs two now.
     * {@code FetchPlanTest} pins those numbers, and this rule stops the annotation drifting back.
     *
     * <p>Compliant:
     * <pre>{@code
     * @ManyToOne(fetch = FetchType.LAZY)
     * private Parent parent;
     * }</pre>
     *
     * <p>Violation (defaults to EAGER):
     * <pre>{@code
     * @ManyToOne
     * private Parent parent;
     * }</pre>
     */
    @ArchTest
    static final ArchRule jpaAssociationsAreLazy =
            //@formatter:off
            classes().that().areAnnotatedWith(Entity.class)
                     .should(new ArchCondition<>("declare every @ManyToOne/@OneToOne as FetchType.LAZY") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             javaClass.getAllFields().forEach(field -> {
                                 if(field.isAnnotatedWith(ManyToOne.class) &&
                                    field.getAnnotationOfType(ManyToOne.class).fetch() != FetchType.LAZY) {
                                     events.add(violation(javaClass, field));
                                 }
                                 if(field.isAnnotatedWith(OneToOne.class) &&
                                    field.getAnnotationOfType(OneToOne.class).fetch() != FetchType.LAZY) {
                                     events.add(violation(javaClass, field));
                                 }
                             });
                         }

                         private static SimpleConditionEvent violation(
                                 final JavaClass javaClass,
                                 final JavaField field
                         ) {
                             return new SimpleConditionEvent(javaClass, false, EAGER_ASSOCIATION_MESSAGE.formatted(
                                     javaClass.getFullName(), field.getName()));
                         }
                     });
    //@formatter:on

    /**
     * No collection association ({@code @OneToMany}, {@code @ManyToMany}) may be fetched eagerly.
     *
     * <p>These default to {@code LAZY}, and an annotation default is indistinguishable from an explicit declaration
     * once compiled, so this rule cannot demand that the fetch type be spelled out. What it does catch is the case that
     * matters: somebody writing {@code FetchType.EAGER} on a collection, which turns every read of the owning entity
     * into an unbounded load of its children.
     *
     * <p>Compliant: {@code @ManyToMany(fetch = FetchType.LAZY) private Set<Teacher> teachers;}
     *
     * <p>Violation: {@code @ManyToMany(fetch = FetchType.EAGER) private Set<Teacher> teachers;}
     */
    @ArchTest
    static final ArchRule jpaCollectionsAreNotEager =
            //@formatter:off
            classes().that().areAnnotatedWith(Entity.class)
                     .should(new ArchCondition<>("never fetch a @OneToMany/@ManyToMany eagerly") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             javaClass.getAllFields().forEach(field -> {
                                 if(field.isAnnotatedWith(OneToMany.class) &&
                                    field.getAnnotationOfType(OneToMany.class).fetch() != FetchType.LAZY) {
                                     events.add(violation(javaClass, field));
                                 }
                                 if(field.isAnnotatedWith(ManyToMany.class) &&
                                    field.getAnnotationOfType(ManyToMany.class).fetch() != FetchType.LAZY) {
                                     events.add(violation(javaClass, field));
                                 }
                             });
                         }

                         private static SimpleConditionEvent violation(
                                 final JavaClass javaClass,
                                 final JavaField field
                         ) {
                             return new SimpleConditionEvent(javaClass, false, EAGER_ASSOCIATION_MESSAGE.formatted(
                                     javaClass.getFullName(), field.getName()));
                         }
                     });
    //@formatter:on

    /**
     * A {@code @OneToMany} must name its inverse with {@code mappedBy}; it may not carry a {@code @JoinColumn}.
     *
     * <p>A {@code @OneToMany} with {@code @JoinColumn} maps the same foreign-key column as the {@code @ManyToOne} on
     * the other side, with neither side declared the inverse of the other. Writing through the collection then costs an
     * extra UPDATE, leaves the two sides disagreeing in memory, and never assigns the generated id back to the caller's
     * instance. {@code mappedBy} gives the column one owner.
     *
     * <p>Compliant:
     * <pre>{@code
     * @OneToMany(mappedBy = "school", fetch = FetchType.LAZY)
     * private Set<Teacher> teachers;
     * }</pre>
     *
     * <p>Violation:
     * <pre>{@code
     * @OneToMany(fetch = FetchType.LAZY)
     * @JoinColumn(name = "SCHOOL_ID")
     * private Set<Teacher> teachers;
     * }</pre>
     */
    @ArchTest
    static final ArchRule jpaOneToManyCollectionsAreMappedByTheirInverse =
            //@formatter:off
            classes().that().areAnnotatedWith(Entity.class)
                     .should(new ArchCondition<>("map every @OneToMany with mappedBy rather than @JoinColumn") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             javaClass.getAllFields().stream()
                                      .filter(field -> field.isAnnotatedWith(OneToMany.class))
                                      .filter(field -> field.isAnnotatedWith(JoinColumn.class) ||
                                                       field.getAnnotationOfType(OneToMany.class).mappedBy().isEmpty())
                                      .forEach(field -> events.add(new SimpleConditionEvent(javaClass, false,
                                              ONE_TO_MANY_WITH_JOIN_COLUMN_MESSAGE.formatted(javaClass.getFullName(),
                                                                                             field.getName()))));
                         }
                     });
    //@formatter:on

    /**
     * An entity's {@code toString()} may not read any association field.
     *
     * <p>Now that associations are lazy, printing one either fires an extra query or, on a detached entity, throws
     * {@code LazyInitializationException} - from inside a log statement, where neither belongs. Lombok's
     * {@code @ToString.Exclude} is the fix, but it is {@code RetentionPolicy.SOURCE} and therefore invisible here, so
     * this rule inspects the generated method instead of the annotation. That is the stronger check anyway: it fails on
     * a hand-written {@code toString()} too.
     *
     * <p>Lombok reads fields through their getters, so both a direct field access and a call to an association's
     * getter count as printing it. The check is deliberately a direct one: an accessor that reads an association on the
     * caller's behalf, the way {@code PersonRole#getName()} reads {@code person}, is not traced through.
     *
     * <p>Compliant:
     * <pre>{@code
     * @ToString.Exclude
     * @ManyToOne(fetch = FetchType.LAZY)
     * private Parent parent;
     * }</pre>
     *
     * <p>Violation (no {@code @ToString.Exclude}, so Lombok reads the field):
     * <pre>{@code
     * @ManyToOne(fetch = FetchType.LAZY)
     * private Parent parent;
     * }</pre>
     */
    @ArchTest
    static final ArchRule jpaEntitiesDoNotPrintAssociations =
            //@formatter:off
            classes().that().areAnnotatedWith(Entity.class)
                     .should(new ArchCondition<>("keep association fields out of toString()") {

                         @Override
                         public void check(final JavaClass javaClass, final ConditionEvents events) {
                             final Map<String, String> byGetterName =
                                     javaClass.getAllFields().stream().filter(JpaEntityRulesTest::isAssociation)
                                              .collect(Collectors.toMap(JpaEntityRulesTest::getterNameFor,
                                                                        JavaField::getName, (first, _) -> first));
                             javaClass.getMethods().stream()
                                      .filter(method -> "toString".equals(method.getName()))
                                      .filter(method -> method.getRawParameterTypes().isEmpty())
                                      .forEach(method -> {
                                          method.getFieldAccesses().stream()
                                                .map(JavaFieldAccess::getTarget)
                                                .flatMap(target -> target.resolveMember().stream())
                                                .filter(JpaEntityRulesTest::isAssociation)
                                                .map(JavaField::getName)
                                                .forEach(name -> events.add(violation(javaClass, name)));
                                          method.getMethodCallsFromSelf().stream()
                                                .map(call -> byGetterName.get(call.getTarget().getName()))
                                                .filter(Objects::nonNull)
                                                .forEach(name -> events.add(violation(javaClass, name)));
                                      });
                         }

                         private static SimpleConditionEvent violation(
                                 final JavaClass javaClass,
                                 final String fieldName
                         ) {
                             return new SimpleConditionEvent(javaClass, false,
                                                             TO_STRING_TOUCHES_ASSOCIATION_MESSAGE.formatted(
                                                                     javaClass.getFullName(), fieldName));
                         }
                     });
    //@formatter:on

    private static boolean isAssociation(final JavaField field) {
        return ASSOCIATION_ANNOTATIONS.stream().anyMatch(field::isAnnotatedWith);
    }

    private static String getterNameFor(final JavaField field) {
        final String name = field.getName();
        return "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

}
