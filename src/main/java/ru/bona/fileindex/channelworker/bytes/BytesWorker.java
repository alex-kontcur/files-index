package ru.bona.fileindex.channelworker.bytes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.model.fileparser.ChannelHandler;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * BytesWorker
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public class BytesWorker implements ChannelWorker {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(BytesWorker.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private BytesChannel bytesChannel;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public BytesWorker() {
        bytesChannel = new BytesChannel();
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public void processOutputChannel(ChannelHandler channelHandler, boolean append) {
        try {
            if (!append) {
                bytesChannel.truncate(0);
            }
            channelHandler.handle(bytesChannel);
        } catch (Exception e) {
            logger.error("Error while process output for channel -> ", e);
        }
    }

    @Override
    public void processInputChannel(ChannelHandler channelHandler) {
        try {
            channelHandler.handle(bytesChannel);
        } catch (Exception e) {
            logger.error("Error while process input for channel ->", e.getMessage());
        }
    }

    @Override
    public void cut(long start, long stop, ReadWriteLock indexLock) {
        try {
            bytesChannel.cut(start, stop);
        } catch (Exception e) {
            logger.error("Error while cut from channel ->", e);
        }
    }
}
