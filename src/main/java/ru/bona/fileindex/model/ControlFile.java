package ru.bona.fileindex.model;

import com.google.inject.Inject;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.filesholder.FilesHolder;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.utils.Helper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;

/**
 * ControlFile
 *
 * @author Kontsur Alex (bona)
 * @since 24.09.14
 */
public abstract class ControlFile {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    protected FilesHolder filesHolder;

    /*===========================================[ CLASS METHODS ]================*/

    @Inject
    public void setup(FilesHolder filesHolder) {
        this.filesHolder = filesHolder;
    }

    protected void saveFileFrameMap(SeekableByteChannel channel, Map<Number, IntRange> pairMap) throws IOException {
        long filesCount = filesHolder.getFilesCount();
        for (Map.Entry<Number, IntRange> frameEntry : pairMap.entrySet()) {

            Number fileNum = frameEntry.getKey();
            ByteBuffer valueBuffer = Helper.getByteBuffer(filesCount, fileNum);
            saveBuffer(channel, valueBuffer);

            IntRange range = frameEntry.getValue();
            ByteBuffer byteBuffer = range.dumpRange();
            saveBuffer(channel, byteBuffer);
        }
    }

    protected static void saveBuffer(SeekableByteChannel channel, ByteBuffer valueBuffer) throws IOException {
        valueBuffer.flip();
        channel.write(valueBuffer);
        valueBuffer.compact();
    }

    protected void fillRangeMap(Map<Number, IntRange> map, SeekableByteChannel channel, Number filesCount) throws IOException {
        for (long i = 0; i < filesCount.longValue(); i++) {
            Number fileNum = Helper.readNumFromChannel(filesHolder.getFilesCount(), channel);
            FileInfo fileInfo = filesHolder.getFileInfo(fileNum);
            IntRange intRange = new IntRange();
            ByteBuffer buffer = ByteBuffer.allocate(intRange.getSize());
            channel.read(buffer);
            buffer.flip();
            intRange.readRange(buffer);

            map.put(fileNum, intRange);
        }
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    public abstract void writeExternal(SeekableByteChannel channel) throws IOException;

    public abstract void readExternal(SeekableByteChannel channel) throws IOException;

}
