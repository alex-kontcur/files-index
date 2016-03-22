package ru.bona.fileindex.listeners;

import ru.bona.fileindex.index.IndexingListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * IdxListener
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public class IdxListener implements IndexingListener {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private CountDownLatch latch;
    private List<File> files;
    private Throwable throwable;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public IdxListener(CountDownLatch latch) {
        this.latch = latch;

        files = new ArrayList<>();
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public void onSuccess(File file) {
        try {
            files.add(file);
        } finally {
            latch.countDown();
        }
    }
    
    @Override
    public void onFailure(File file, Throwable t) {
        try {
            throwable = t;
        } finally {
            latch.countDown();
        }
    }

    /*===========================================[ GETTER/SETTER ]================*/

    public List<File> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
