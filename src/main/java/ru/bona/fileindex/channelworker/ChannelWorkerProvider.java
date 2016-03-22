package ru.bona.fileindex.channelworker;

/**
 * ChannelWorkerProvider
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public interface ChannelWorkerProvider {

    /*===========================================[ INTERFACE METHODS ]============*/

    ChannelWorker getChannelWorker(String fileName);

    void clearWorkers();
}
