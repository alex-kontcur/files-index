package ru.bona.fileindex.model.lexem;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.filesholder.FilesHolder;
import ru.bona.fileindex.model.Storable;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.model.range.Range;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * LexemeIndexFrame
 *
 * @author Kontsur Alex (bona)
 * @since 22.09.14
 */
public class LexemeIndexFrame implements Storable<SeekableByteChannel> {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    //тут лежат диапазоны одной лексемы в одном файле
    private SortedSet<Range> lexemRanges;
    private LexemeRegistry lexemeRegistry;
    private AtomicBoolean stored;
    private Number fileNum;
    private String lexeme;

    /*===========================================[ CONSTRUCTORS ]=================*/

    @Inject
    public LexemeIndexFrame(@Assisted String lexeme, @Assisted Number fileNum, @Assisted SortedSet<Range> lexemRanges,
                            LexemeRegistry lexemeRegistry, FilesHolder filesHolder) {
        this.lexeme = lexeme;
        this.fileNum = fileNum;
        this.lexemeRegistry = lexemeRegistry;
        this.lexemRanges = lexemRanges == null ? new TreeSet() : new TreeSet(lexemRanges);

        stored = new AtomicBoolean(false);
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    public String getLexeme() {
        return lexeme;
    }

    @Override
    public boolean isStored() {
        return stored.get();
    }

    private byte[] getRangesBytes() {
        int size = 0;
        for (Range lexemRange : lexemRanges) {
            size += lexemRange.getSize();
        }
        byte[] arr = new byte[size];
        int i = 0;
        for (Range lexemRange : lexemRanges) {
            ByteBuffer buffer = lexemRange.dumpRange();
            buffer.flip();
            while (buffer.hasRemaining()) {
                arr[i++] = buffer.get();
            }
        }
        return arr;
    }

    public void dropPreviousFrames(ChannelWorker channelWorker, ReadWriteLock indexLock) {
        Range previousRange = lexemeRegistry.getPreviousFrame(lexeme, fileNum);
        if (previousRange != null) {
            // Если мы тут это значит, что спец реестр уже содержит предыдущий регион
            // по этому спец символу для этого файла. Перед сохранением нового, нужно удалить все предыдущее
            // 1. Сначала удаляем регион в индексе
            long start = previousRange.getStart().longValue();
            long stop = previousRange.getStop().longValue();

            channelWorker.cut(start, stop, indexLock);

            // 2. Чистим реестр
            lexemeRegistry.removeFrameInfosForLexeme(lexeme, fileNum);
        }
    }

    @Override
    public void store(SeekableByteChannel channel, ReadWriteLock indexLock) throws IOException {
        stored.set(false);

        AtomicLong start = new AtomicLong(0);
        AtomicLong stop = new AtomicLong(0);

        indexLock.writeLock().lock();
        try {
            start.set(channel.size());

            byte[] bytes = getRangesBytes();
            ByteBuffer fullBuffer = ByteBuffer.allocate(bytes.length);
            fullBuffer.put(bytes);

            fullBuffer.flip();
            channel.write(fullBuffer);
            fullBuffer.compact();
            stop.set(channel.size());

            IntRange range = new IntRange();
            range.setStart(start.get());
            range.setStop(stop.get());

            Map<Number, IntRange> frameMap = new HashMap<>();
            frameMap.put(fileNum, range);
            lexemeRegistry.register(lexeme, frameMap);

        } finally {
            indexLock.writeLock().unlock();
        }

        stored.set(true);
    }

}
