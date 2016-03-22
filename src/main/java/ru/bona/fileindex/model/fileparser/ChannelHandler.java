package ru.bona.fileindex.model.fileparser;

import java.nio.channels.SeekableByteChannel;

/**
* ChannelHandler
*
* @author Kontsur Alex (bona)
* @since 21.09.14
*/
public interface ChannelHandler {

    /*===========================================[ INTERFACE METHODS ]============*/

    void handle(SeekableByteChannel channel) throws Exception;

}
