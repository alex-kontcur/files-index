package ru.bona.fileindex.channelworker;

import ru.bona.fileindex.channelworker.bytes.BWorker;
import ru.bona.fileindex.channelworker.file.FWorker;

/**
 * ChannelWorkerFactory
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public interface ChannelWorkerFactory {

    /*===========================================[ CLASS METHODS ]================*/

    @FWorker
    ChannelWorker createWorker(String fileName);
    
    @BWorker
    ChannelWorker createWorker();
    
}
