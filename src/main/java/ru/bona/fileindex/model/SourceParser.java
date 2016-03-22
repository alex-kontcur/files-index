package ru.bona.fileindex.model;

import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.model.fileparser.CharBufferAction;

import java.nio.channels.Channel;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SourceParser
 *
 * @author Kontsur Alex (bona)
 * @since 22.09.14
 */
public interface SourceParser<S extends Channel> {

    /*===========================================[ INTERFACE METHODS ]============*/

    BufferInfo readChars(ChannelWorker channelWorker, String encoding, long start, long stop);

    BufferInfo readChar(ChannelWorker channelWorker, String encoding, long stop);

    void processSourceByPieces(FileInfo fileInfo, CharBufferAction action);

    void fillWords(String source, Map<String, AtomicInteger> words);
}
