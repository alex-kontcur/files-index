package ru.bona.fileindex.channelworker;

import ru.bona.fileindex.model.fileparser.ChannelHandler;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * ChannelWorker
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public interface ChannelWorker {

    /*===========================================[ CLASS METHODS ]================*/

    void processOutputChannel(ChannelHandler channelHandler, boolean append);

    void processInputChannel(ChannelHandler channelHandler);

    void cut(long start, long stop, ReadWriteLock indexLock);
    
}
