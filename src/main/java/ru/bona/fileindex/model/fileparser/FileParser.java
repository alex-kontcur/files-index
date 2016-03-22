package ru.bona.fileindex.model.fileparser;

import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.channelworker.ChannelWorkerProvider;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.model.BufferInfo;
import ru.bona.fileindex.model.SourceParser;
import ru.bona.fileindex.model.spec.SpecRegistry;
import ru.bona.fileindex.utils.Helper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FileParser
 *
 * @author Kontsur Alex (bona)
 * @since 22.09.14
 */
public class FileParser implements SourceParser<FileChannel> {

    /*===========================================[ STATIC VARIABLES ]=============*/

    public static final int CHAR_BUFFER_SIZE = 1024 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(FileParser.class);

    private ChannelWorkerProvider channelWorkerProvider;

    /*===========================================[ CONSTRUCTORS ]=================*/

    @Inject
    protected void init(ChannelWorkerProvider channelWorkerProvider) {
        this.channelWorkerProvider = channelWorkerProvider;
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public BufferInfo readChar(ChannelWorker channelWorker, String encoding, long stop) {
        return readChars(channelWorker, encoding, stop - 1, stop);
    }

    @Override
    public BufferInfo readChars(ChannelWorker channelWorker, final String encoding, final long start, final long stop) {
        Validate.isTrue(start < stop);
        final List<BufferInfo> result = new ArrayList<>();
        try {
            channelWorker.processInputChannel(new ChannelHandler() {
                @Override
                public void handle(SeekableByteChannel channel) throws Exception {
                    CharsetDecoder decoder = Helper.prepareDecoder(encoding);
                    int capacity = (int) (stop - start);
                    ByteBuffer data = ByteBuffer.allocate(capacity);
                    CharBuffer chBuffer = CharBuffer.allocate(capacity);
                    channel.position(start < 0 ? 0 : start);
                    result.add(extractChars(channel, decoder, data, chBuffer));
                }
            });
        } catch (Exception e) {
            logger.error("Error while  ->", e);
        }
        return result.isEmpty() ? new BufferInfo(CharBuffer.allocate(0), -1, -1) : result.get(0);
    }

    @Override
    public void processSourceByPieces(final FileInfo fileInfo, final CharBufferAction action) {
        String fileName = fileInfo.getFullPath();
        ChannelWorker channelWorker = channelWorkerProvider.getChannelWorker(fileName);
        try {
            channelWorker.processInputChannel(new ChannelHandler() {
                @Override
                public void handle(SeekableByteChannel channel) throws Exception {
                    String encoding = fileInfo.getEncoding();
                    CharsetDecoder decoder = Helper.prepareDecoder(encoding);
                    ByteBuffer data = ByteBuffer.allocate(getCharBufferSize());
                    CharBuffer chBuffer = CharBuffer.allocate(getCharBufferSize());
                    long channelSize = channel.size();
                    channel.position(0);
                    while (channel.position() < channelSize) {
                        BufferInfo bufferInfo = extractChars(channel, decoder, data, chBuffer);
                        if (channel.position() == channelSize) {
                            bufferInfo.setLast(true);
                        }
                        action.processBuffer(bufferInfo, fileInfo);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Error while processing input channel for \"" + fileName + "\" -> ", e);
        }
    }

    public int getCharBufferSize() {
        return CHAR_BUFFER_SIZE;
    }

    public static BufferInfo extractChars(SeekableByteChannel channel, CharsetDecoder decoder,
                                          ByteBuffer data, CharBuffer chBuffer) throws IOException {
        long start = channel.position();
        channel.read(data);
        long stop = channel.position();

        BufferInfo bufferInfo = getBufferInfo(data, decoder, chBuffer, start, stop);

        data.clear();
        chBuffer.clear();
        return bufferInfo;
    }

    private static BufferInfo getBufferInfo(ByteBuffer data, CharsetDecoder decoder,
                                            CharBuffer chBuffer, long start, long stop) {
        CoderResult result;
        decoder.reset();
        data.flip();
        do {
            result = decoder.decode(data, chBuffer, false);
            chBuffer.flip();
            chBuffer.clear();
            if (result.isUnderflow()) {
                break;
            }
        } while (result.isOverflow());

        data.compact();

        return new BufferInfo(chBuffer, start, stop);
    }

    @Override
    public void fillWords(String source, Map<String, AtomicInteger> words) {
        String filtered = SpecRegistry.cleanSource(source);
        String[] parts = filtered.split(" ");
        for (String part : parts) {
            AtomicInteger counter = words.get(part);
            if (counter == null) {
                counter = new AtomicInteger(1);
                words.put(part, counter);
            }
            counter.incrementAndGet();
        }
    }

}
