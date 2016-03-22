package ru.bona.fileindex.model.spec;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.filesholder.FilesHolder;
import ru.bona.fileindex.model.Loadable;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.model.range.Range;
import ru.bona.fileindex.model.spec.synctree.SyncTree;
import ru.bona.fileindex.utils.Helper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * SpecEntry
 *
 * @author Kontsur Alex (bona)
 * @since 23.09.14
 */
public class SpecEntry implements Loadable<SeekableByteChannel> {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private Character character;
    private Map<Number, SyncTree> fileTreeMap;

    private FilesHolder filesHolder;

    /*===========================================[ CONSTRUCTORS ]=================*/

    @Inject
    public SpecEntry(@Assisted Character character, FilesHolder filesHolder) {
        this.character = character;
        this.filesHolder = filesHolder;

        fileTreeMap = new ConcurrentHashMap<>();
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    public SyncTree getTree(Number fileNum) {
        return fileTreeMap.get(fileNum);
    }

    public Character getCharacter() {
        return character;
    }

    public Collection<SyncTree> getCharacterTrees() {
        return new ArrayList<>(fileTreeMap.values());
    }

    @Override
    public void load(SeekableByteChannel channel, ReadWriteLock indexLock, Map<Number, IntRange> frameInfoMap) throws IOException {
        for (Map.Entry<Number, IntRange> infoEntry : frameInfoMap.entrySet()) {
            Number fileNumber = infoEntry.getKey();
            FileInfo fileInfo = filesHolder.getFileInfo(fileNumber);
            long fileSize = fileInfo.getFileSize();
            SyncTree syncTree = Helper.makeSyncTree(fileSize);

            Range range = infoEntry.getValue();
            long start = range.getStart().longValue();
            long stop = range.getStop().longValue();

            ByteBuffer buffer = ByteBuffer.allocate((int) (stop - start));
            indexLock.readLock().lock();
            try {
                channel.position(start);
                channel.read(buffer);
                buffer.flip();
            } finally {
                indexLock.readLock().unlock();
            }
            syncTree.readTree(buffer);

            fileTreeMap.put(fileNumber, syncTree);
        }
    }
}
