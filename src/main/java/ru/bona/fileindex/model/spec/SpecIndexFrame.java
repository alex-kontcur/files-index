package ru.bona.fileindex.model.spec;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.model.Storable;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.model.range.Range;
import ru.bona.fileindex.model.spec.synctree.SyncTree;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * SpecIndexFrame
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */
public class SpecIndexFrame implements Storable<SeekableByteChannel> {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(SpecIndexFrame.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private Map<Character, SyncTree> treeMap;
    private SpecRegistry specRegistry;
    private AtomicBoolean stored;
    private Number fileNum;

    /*===========================================[ CONSTRUCTORS ]=================*/

    @Inject
    public SpecIndexFrame(@Assisted Number fileNum, @Assisted Map<Character, SyncTree> treeMap, SpecRegistry specRegistry) {
        this.fileNum = fileNum;
        this.specRegistry = specRegistry;
        this.treeMap = treeMap == null ? new ConcurrentHashMap() : new ConcurrentHashMap<>(treeMap);

        stored = new AtomicBoolean(false);
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public boolean isStored() {
        return stored.get();
    }

    public void dropPreviousFrames(ChannelWorker channelWorker, ReadWriteLock indexLock) {
        for (Map.Entry<Character, SyncTree> entry : treeMap.entrySet()) {
            Character character = entry.getKey();
            Range previousFrame = specRegistry.getPreviousFrame(character, fileNum);
            if (previousFrame != null) {
                // Если мы тут это значит, что спец реестр уже содержит предыдущий регион
                // по этому спец символу для этого файла. Перед сохранением нового, нужно удалить все предыдущее
                // 1. Сначала удаляем регион в индексе
                long start = previousFrame.getStart().longValue();
                long stop = previousFrame.getStop().longValue();

                channelWorker.cut(start, stop, indexLock);

                // 2. Чистим реестр
                specRegistry.delete(character, fileNum);
            }
        }
    }

    @Override
    public void store(SeekableByteChannel channel, ReadWriteLock indexLock) throws IOException {
        stored.set(false);

        AtomicLong start = new AtomicLong(0);
        AtomicLong stop = new AtomicLong(0);

        for (Map.Entry<Character, SyncTree> entry : treeMap.entrySet()) {
            Character character = entry.getKey();
            SyncTree syncTree = entry.getValue();

            indexLock.writeLock().lock();
            try {
                start.set(channel.size());
                ByteBuffer byteBuffer = syncTree.dumpTree();
                byteBuffer.flip();
                channel.write(byteBuffer);
                byteBuffer.compact();
                stop.set(channel.size());
            } finally {
                indexLock.writeLock().unlock();
            }

            IntRange range = new IntRange();
            range.setStart(start.get());
            range.setStop(stop.get());

            Map<Number, IntRange> frameMap = new HashMap<>();
            frameMap.put(fileNum, range);
            specRegistry.register(character, frameMap);
        }
        stored.set(true);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SpecIndexFrame frame = (SpecIndexFrame) obj;
        if (!fileNum.equals(frame.fileNum)) {
            return false;
        }
        return CollectionUtils.isEqualCollection(treeMap.values(), frame.treeMap.values());
    }

    @Override
    public int hashCode() {
        int result = treeMap.values().hashCode();
        result = 31 * result + fileNum.hashCode();
        return result;
    }
}
