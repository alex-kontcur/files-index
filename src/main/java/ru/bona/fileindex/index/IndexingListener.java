package ru.bona.fileindex.index;

import java.io.File;

/**
 * IndexingListener
 *
 * @author Kontsur Alex (bona)
 * @since 18.09.14
 */
public interface IndexingListener {

    /*===========================================[ CLASS METHODS ]================*/

    void onSuccess(File file);

    void onFailure(File file, Throwable t);

}
