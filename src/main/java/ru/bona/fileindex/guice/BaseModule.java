package ru.bona.fileindex.guice;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.inject.*;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.channelworker.ChannelWorkerFactory;
import ru.bona.fileindex.channelworker.ChannelWorkerProvider;
import ru.bona.fileindex.channelworker.bytes.BWorker;
import ru.bona.fileindex.channelworker.bytes.BytesWorker;
import ru.bona.fileindex.channelworker.file.FWorker;
import ru.bona.fileindex.channelworker.file.FileWorker;
import ru.bona.fileindex.channelworker.file.FileWorkerProvider;
import ru.bona.fileindex.filesholder.FilesHolder;
import ru.bona.fileindex.index.IndexService;
import ru.bona.fileindex.index.builder.IndexBuilder;
import ru.bona.fileindex.index.builder.IndexBuilderFactory;
import ru.bona.fileindex.model.SourceAnalyzer;
import ru.bona.fileindex.model.SourceParser;
import ru.bona.fileindex.model.fileparser.FileParser;
import ru.bona.fileindex.model.lexem.*;
import ru.bona.fileindex.model.lexem.factory.LexemEntryFactory;
import ru.bona.fileindex.model.lexem.factory.LexemeIndexFrameFactory;
import ru.bona.fileindex.model.spec.*;
import ru.bona.fileindex.model.spec.factory.SpecEntryFactory;
import ru.bona.fileindex.model.spec.factory.SpecIndexFrameFactory;
import ru.bona.fileindex.search.SearchService;
import ru.bona.fileindex.search.searcher.IndexSearcher;
import ru.bona.fileindex.search.searcher.IndexSearcherFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * BaseModule
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
@SuppressWarnings({"OverlyCoupledMethod", "OverlyCoupledClass"})
public class BaseModule extends AbstractModule {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private Configuration configuration;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public BaseModule(Configuration configuration) {
        this.configuration = configuration;
    }

    /*===========================================[ CLASS METHODS ]================*/

    @Provides
    public Configuration getConfiguration() {
        return configuration;
    }

    @Inject
    @Provides
    @SourceAnalyzers
    public Collection<SourceAnalyzer> getSourceAnalyzer(final Injector injector) {
        List<Class<? extends SourceAnalyzer>> analyzers = new ArrayList<>();

        analyzers.add(LexemeAnalyzer.class);
        analyzers.add(SpecAnalyzer.class);

        return Collections2.transform(analyzers,
            new Function<Class<? extends SourceAnalyzer>, SourceAnalyzer>() {
                @Override
                public SourceAnalyzer apply(Class<? extends SourceAnalyzer> input) {
                    return injector.getInstance(input);
                }
            }
        );
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    protected void configure() {

        install(new FactoryModuleBuilder()
            .implement(ChannelWorker.class, BWorker.class, BytesWorker.class)
            .implement(ChannelWorker.class, FWorker.class, FileWorker.class)
            .build(ChannelWorkerFactory.class));

        install(new FactoryModuleBuilder().implement(IndexBuilder.class, IndexBuilder.class).build(IndexBuilderFactory.class));
        install(new FactoryModuleBuilder().implement(IndexSearcher.class, IndexSearcher.class).build(IndexSearcherFactory.class));

        install(new FactoryModuleBuilder().implement(LexemeIndexFrame.class, LexemeIndexFrame.class).build(LexemeIndexFrameFactory.class));
        install(new FactoryModuleBuilder().implement(LexemEntry.class, LexemEntry.class).build(LexemEntryFactory.class));
        install(new FactoryModuleBuilder().implement(SpecIndexFrame.class, SpecIndexFrame.class).build(SpecIndexFrameFactory.class));
        install(new FactoryModuleBuilder().implement(SpecEntry.class, SpecEntry.class).build(SpecEntryFactory.class));

        bindChannelProvider();
        initSourceParser();

        bind(FilesHolder.class).asEagerSingleton();
        bind(IndexService.class).in(Scopes.SINGLETON);
        bind(SearchService.class).in(Scopes.SINGLETON);

        bind(LexemControlFile.class).in(Scopes.SINGLETON);
        bind(LexemeAnalyzer.class).in(Scopes.SINGLETON);
        bind(LexemeFrameManager.class).in(Scopes.SINGLETON);
        bind(LexemeRegistry.class).in(Scopes.SINGLETON);

        bind(SpecControlFile.class).in(Scopes.SINGLETON);
        bind(SpecAnalyzer.class).in(Scopes.SINGLETON);
        bind(SpecFrameManager.class).in(Scopes.SINGLETON);
        bind(SpecRegistry.class).in(Scopes.SINGLETON);
    }

    protected void bindChannelProvider() {
        bind(ChannelWorkerProvider.class).to(FileWorkerProvider.class);
        bind(FileWorkerProvider.class).in(Scopes.SINGLETON);
    }

    protected void initSourceParser() {
        bind(SourceParser.class).to(FileParser.class);
    }
}
