package ru.bona.fileindex.model.lexem;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.filesholder.FilesHolder;
import ru.bona.fileindex.model.Loadable;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.model.range.Range;
import ru.bona.fileindex.utils.Helper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * LexemEntry
 *
 * @author Kontsur Alex (bona)
 * @since 23.09.14
 */
public class LexemEntry implements Loadable<SeekableByteChannel> {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private String lexeme;
    private FilesHolder filesHolder;
    private Map<Number, SortedSet<Range>> lexemeFileMap;

    /*===========================================[ CONSTRUCTORS ]=================*/

    @Inject
    public LexemEntry(@Assisted String lexeme, FilesHolder filesHolder) {
        this.lexeme = lexeme;
        this.filesHolder = filesHolder;
        lexemeFileMap = new ConcurrentHashMap<>();
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    public SortedSet<Range> getRanges(Number fileNum) {
        return lexemeFileMap.get(fileNum);
    }

    public String getLexeme() {
        return lexeme;
    }

    public Map<Number, SortedSet<Range>> getLexemeFileMap() {
        return Collections.unmodifiableMap(lexemeFileMap);
    }

    @Override
    public void load(SeekableByteChannel channel, ReadWriteLock indexLock, Map<Number, IntRange> frameInfoMap) throws IOException {
        lexemeFileMap.clear();
        for (Map.Entry<Number, IntRange> infoEntry : frameInfoMap.entrySet()) {
            Number fileNum = infoEntry.getKey();
            FileInfo fileInfo = filesHolder.getFileInfo(fileNum);
            long fileSize = fileInfo.getFileSize();

            Range range = infoEntry.getValue();
            long start = range.getStart().intValue();
            long stop = range.getStop().intValue();

            ByteBuffer buffer = ByteBuffer.allocate((int) (stop - start));
            indexLock.readLock().lock();
            try {
                channel.position(start);
                channel.read(buffer);
                buffer.flip();
            } finally {
                indexLock.readLock().unlock();
            }

            SortedSet<Range> set = new TreeSet<>();
            while (buffer.hasRemaining()) {
                Range lexemRange = Helper.makeRange(fileSize);
                lexemRange.readRange(buffer);
                set.add(lexemRange);
            }
            lexemeFileMap.put(fileNum, set);
        }
    }
}
