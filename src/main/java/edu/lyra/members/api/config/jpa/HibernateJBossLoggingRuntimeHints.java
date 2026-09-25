package edu.lyra.members.api.config.jpa;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers native-image reflection access for the {@code *_$logger} classes JBoss Logging's annotation processor
 * generates, at compile time, for every {@code @MessageLogger} interface Hibernate declares (e.g.
 * {@code CoreMessageLogger_$logger}, {@code JpaLogger_$logger}). {@code org.jboss.logging.Logger} looks these up by
 * name at runtime ({@code MethodHandles.Lookup#findClass}) rather than referencing them directly, so native-image's
 * static analysis has no way to know they're reachable without this: left unregistered, the first one touched at
 * startup fails with {@code IllegalArgumentException: Invalid logger interface ... (implementation not found)}.
 *
 * @author Esteban Cristóbal Rodríguez
 */
class HibernateJBossLoggingRuntimeHints
        implements RuntimeHintsRegistrar {

    // Every org.hibernate.**.*_$logger class present in hibernate-core 7.4.5.Final.
    private static final String[] GENERATED_LOGGER_CLASSES = {
            "org.hibernate.action.internal.ActionLogging_$logger",
            "org.hibernate.boot.BootLogging_$logger",
            "org.hibernate.boot.archive.scan.internal.ScannerLogger_$logger",
            "org.hibernate.boot.beanvalidation.BeanValidationLogger_$logger",
            "org.hibernate.boot.jaxb.JaxbLogger_$logger",
            "org.hibernate.bytecode.enhance.internal.BytecodeEnhancementLogging_$logger",
            "org.hibernate.bytecode.enhance.spi.interceptor.BytecodeInterceptorLogging_$logger",
            "org.hibernate.cache.spi.SecondLevelCacheLogger_$logger",
            "org.hibernate.collection.internal.CollectionLogger_$logger",
            "org.hibernate.context.internal.CurrentSessionLogging_$logger",
            "org.hibernate.dialect.DialectLogging_$logger",
            "org.hibernate.engine.internal.NaturalIdLogging_$logger",
            "org.hibernate.engine.internal.PersistenceContextLogging_$logger",
            "org.hibernate.engine.internal.SessionMetricsLogger_$logger",
            "org.hibernate.engine.internal.VersionLogger_$logger",
            "org.hibernate.engine.jdbc.JdbcLogging_$logger",
            "org.hibernate.engine.jdbc.batch.JdbcBatchLogging_$logger",
            "org.hibernate.engine.jdbc.connections.internal.ConnectionProviderLogging_$logger",
            "org.hibernate.engine.jdbc.env.internal.LobCreationLogging_$logger",
            "org.hibernate.engine.jdbc.spi.SQLExceptionLogging_$logger",
            "org.hibernate.event.internal.EntityCopyLogging_$logger",
            "org.hibernate.event.internal.EventListenerLogging_$logger",
            "org.hibernate.id.UUIDLogger_$logger",
            "org.hibernate.id.enhanced.OptimizerLogger_$logger",
            "org.hibernate.id.enhanced.SequenceGeneratorLogger_$logger",
            "org.hibernate.id.enhanced.TableGeneratorLogger_$logger",
            "org.hibernate.internal.CoreMessageLogger_$logger",
            "org.hibernate.internal.SessionFactoryLogging_$logger",
            "org.hibernate.internal.SessionFactoryRegistryMessageLogger_$logger",
            "org.hibernate.internal.SessionLogging_$logger",
            "org.hibernate.internal.log.ConnectionAccessLogger_$logger",
            "org.hibernate.internal.log.ConnectionInfoLogger_$logger",
            "org.hibernate.internal.log.DeprecationLogger_$logger",
            "org.hibernate.internal.log.IncubationLogger_$logger",
            "org.hibernate.internal.log.StatisticsLogger_$logger",
            "org.hibernate.internal.log.UrlMessageBundle_$logger",
            "org.hibernate.jpa.internal.JpaLogger_$logger",
            "org.hibernate.loader.ast.internal.MultiKeyLoadLogging_$logger",
            "org.hibernate.metamodel.mapping.MappingModelCreationLogging_$logger",
            "org.hibernate.query.QueryLogging_$logger",
            "org.hibernate.query.hql.HqlLogging_$logger",
            "org.hibernate.resource.beans.internal.BeansMessageLogger_$logger",
            "org.hibernate.resource.jdbc.internal.LogicalConnectionLogging_$logger",
            "org.hibernate.resource.jdbc.internal.ResourceRegistryLogger_$logger",
            "org.hibernate.resource.transaction.backend.jta.internal.JtaLogging_$logger",
            "org.hibernate.resource.transaction.internal.SynchronizationLogging_$logger",
            "org.hibernate.service.internal.ServiceLogger_$logger",
            "org.hibernate.sql.ast.tree.SqlAstTreeLogger_$logger",
            "org.hibernate.sql.exec.SqlExecLogger_$logger",
            "org.hibernate.sql.model.ModelMutationLogging_$logger",
            "org.hibernate.sql.results.LoadingLogger_$logger",
            "org.hibernate.sql.results.ResultsLogger_$logger",
            "org.hibernate.sql.results.graph.embeddable.EmbeddableLoadingLogger_$logger",
    };

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        for(final String className : GENERATED_LOGGER_CLASSES) {
            final MemberCategory invokeConstructors = MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
            hints.reflection().registerTypeIfPresent(classLoader, className, invokeConstructors);
        }
    }

}
