package ru.bona.fileindex.channelworker.file;

import ru.bona.fileindex.channelworker.AbstractWorkerProvider;
import ru.bona.fileindex.channelworker.ChannelWorker;

/**
 * FileWorkerProvider
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public class FileWorkerProvider extends AbstractWorkerProvider {

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    protected ChannelWorker createWorker(String fileName) {
        return factory.createWorker(fileName);
    }
}
