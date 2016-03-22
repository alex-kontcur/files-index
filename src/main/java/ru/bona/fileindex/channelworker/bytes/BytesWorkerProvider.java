package ru.bona.fileindex.channelworker.bytes;

import ru.bona.fileindex.channelworker.AbstractWorkerProvider;
import ru.bona.fileindex.channelworker.ChannelWorker;

/**
 * BytesWorkerProvider
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public class BytesWorkerProvider extends AbstractWorkerProvider {

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    protected ChannelWorker createWorker(String fileName) {
        return factory.createWorker();
    }
}
