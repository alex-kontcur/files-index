package ru.bona.fileindex.model;

import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.model.fileparser.ChannelHandler;

import java.nio.channels.SeekableByteChannel;

/**
 * Registry
 *
 * @author Kontsur Alex (bona)
 * @since 24.09.14
 */
public abstract class Registry<C extends ControlFile> {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    protected C controlFile;
    protected ChannelWorker channelWorker;

    /*===========================================[ CLASS METHODS ]================*/

    public void createDump() {
        channelWorker.processOutputChannel(new ChannelHandler() {
            @Override
            public void handle(SeekableByteChannel channel) throws Exception {
                channel.truncate(0);
                controlFile.writeExternal(channel);
            }
        }, false);
    }

    public abstract C getControlFile();

}
