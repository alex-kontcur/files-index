package ru.bona.fileindex.listeners;

import ru.bona.fileindex.search.FileSearchInfo;
import ru.bona.fileindex.search.SearchCompleteListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

/**
 * SrchListener
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public class SrchListener implements SearchCompleteListener {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private CountDownLatch latch;
    private Throwable throwable;
    private Collection<FileSearchInfo> matchedFileInfos;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public SrchListener(CountDownLatch latch) {
        this.latch = latch;
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public void onSearchComplete(Collection<FileSearchInfo> matchedFileInfos) {
        try {
            this.matchedFileInfos = new ArrayList<>(matchedFileInfos);
        } finally {
            latch.countDown();
        }
    }

    @Override
    public void onFailure(Throwable t) {
        try {
            throwable = t;
            t.printStackTrace();
        } finally {
            latch.countDown();
        }
    }

    /*===========================================[ GETTER/SETTER ]================*/

    public Collection<FileSearchInfo> getMatchedFileInfos() {
        return Collections.unmodifiableCollection(matchedFileInfos);
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
