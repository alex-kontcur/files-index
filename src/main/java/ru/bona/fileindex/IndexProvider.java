package ru.bona.fileindex;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import ru.bona.fileindex.guice.BaseModule;
import ru.bona.fileindex.index.IndexService;
import ru.bona.fileindex.search.SearchService;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * IndexProvider
 *
 * @author Kontsur Alex (bona)
 * @since 27.09.14
 */
@SuppressWarnings("NonThreadSafeLazyInitialization")
public class IndexProvider {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static Lock reentrantLock = new ReentrantLock();
    private static IndexProvider instance;

    public static IndexProvider newInstance(Configuration configuration) {
        if (instance == null) {
            reentrantLock.lock();
            try {
                if (instance == null) {
                    instance = new IndexProvider().createInstance(configuration);
                }
            } finally {
                reentrantLock.unlock();
            }
        }
        return instance;
    }

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private Injector injector;

    /*===========================================[ CLASS METHODS ]================*/

    @Inject
    protected void init(Injector injector) {
        this.injector = injector;
    }

    private IndexProvider createInstance(Configuration configuration) {
        injector = Guice.createInjector(new BaseModule(configuration));
        return injector.getInstance(getClass());
    }

    public IndexService getIndexService() {
        return injector.getInstance(IndexService.class);
    }

    public SearchService getSearchService() {
        return injector.getInstance(SearchService.class);
    }

}
