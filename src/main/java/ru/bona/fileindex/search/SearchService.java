package ru.bona.fileindex.search;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.model.lexem.LexemeAnalyzer;
import ru.bona.fileindex.model.spec.SpecAnalyzer;
import ru.bona.fileindex.search.searcher.IndexSearcher;
import ru.bona.fileindex.search.searcher.IndexSearcherFactory;
import ru.bona.fileindex.utils.Helper;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * SearchService
 *
 * @author Kontsur Alex (bona)
 * @since 25.09.14
 */
public class SearchService {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private SpecAnalyzer specAnalyzer;
    private LexemeAnalyzer lexemeAnalyzer;
    private IndexSearcherFactory indexSearcherFactory;

    private Queue<SearchTask> awaitingTasks;

    private Semaphore semaphore;
    private ExecutorService searchExecutor;

    /*===========================================[ CONSTRUCTORS ]=================*/

    @Inject
    protected void init(SpecAnalyzer specAnalyzer, LexemeAnalyzer lexemeAnalyzer, Configuration configuration,
                        IndexSearcherFactory indexSearcherFactory) {

        this.specAnalyzer = specAnalyzer;
        this.lexemeAnalyzer = lexemeAnalyzer;
        this.indexSearcherFactory = indexSearcherFactory;

        awaitingTasks = new ConcurrentLinkedQueue<>();

        int simThreads = configuration.getSimSearchThreads();
        semaphore = new Semaphore(simThreads);
        searchExecutor = Helper.prepareFilesThreadPool("SRCH-Worker", simThreads);

        ScheduledExecutorService pollExecutor = Executors.newSingleThreadScheduledExecutor();
        pollExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                runSearch();
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    /*===========================================[ CLASS METHODS ]================*/

    public void searchFor(String target, SearchCompleteListener listener) {
        List<CharsPosition> specPositions = specAnalyzer.getCharsPositions(target);
        List<CharsPosition> lexemesPositions = lexemeAnalyzer.getCharsPositions(target);
        SearchRecord record = new SearchRecord(lexemesPositions, specPositions);
        awaitingTasks.add(new SearchTask(target, record, listener));
    }

    private void runSearch() {
        try {
            while (!awaitingTasks.isEmpty()) {
                SearchTask searchTask = awaitingTasks.poll();
                submitTask(searchTask);
                logger.info("Search of {} submitted", searchTask.getSource());
            }
        } catch (Throwable t) {
            logger.error("Error while running order processors : ", t);
        }
    }

    private Future submitTask(SearchTask searchTask) {
        Future future = null;
        try {
            semaphore.acquire();
            final String source = searchTask.getSource();
            final SearchRecord searchRecord = searchTask.getSearchRecord();
            final SearchCompleteListener listener = searchTask.getListener();
            future = searchExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        IndexSearcher indexSearcher = indexSearcherFactory.createIndexSearcher();
                        logger.info("Searching started for \"{}\"", source);
                        Collection<FileSearchInfo> matchedFileInfos = indexSearcher.search(source, searchRecord);
                        logger.info("Searching stopped for for \"{}\"", source);
                        listener.onSearchComplete(matchedFileInfos);
                    } catch (Throwable t) {
                        logger.info("Search failed for \"{}\" -> {}", source, t.getMessage());
                        listener.onFailure(t);
                    }
                    semaphore.release();
                }
            });
        } catch (InterruptedException e) {
            logger.error("Waiting for submitTask is interrupted ->", e);
        }
        return future;
    }

}
