package ru.bona.fileindex.model.spec;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.channelworker.ChannelWorkerProvider;
import ru.bona.fileindex.model.FrameManager;
import ru.bona.fileindex.model.fileparser.ChannelHandler;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.model.range.Range;
import ru.bona.fileindex.model.spec.factory.SpecEntryFactory;
import ru.bona.fileindex.utils.Helper;

import java.io.File;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SpecFrameManager
 *
 * @author Kontsur Alex (bona)
 * @since 22.09.14
 */
public class SpecFrameManager implements FrameManager<SpecEntry, SpecIndexFrame> {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final String SPEC_INDEX_FILE = "spec.idx";
    private static final String SPEC_INDEX_BACKUP_FILE = "spec.idx.bak";

    private static final Logger logger = LoggerFactory.getLogger(SpecFrameManager.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    // Будет использоваться для блокирования параллельных операций на индексном файле
    // например для вырезания сегмента из середины файла. Файл реестра будет бэкапиться
    // на диск по расписанию, создавая при этом актуальную на тот момент копию индексного файла.
    private ReadWriteLock specIndexLock;

    private SpecEntryFactory specEntryFactory;
    private ChannelWorker channelWorker;
    private SpecRegistry specRegistry;
    private String indexPath;

    /*===========================================[ INTERFACE METHODS ]============*/

    @Inject
    protected void init(SpecEntryFactory specEntryFactory, SpecRegistry specRegistry,
                        ChannelWorkerProvider workerProvider, Configuration configuration) {
        this.specEntryFactory = specEntryFactory;
        this.specRegistry = specRegistry;

        specIndexLock = new ReentrantReadWriteLock();

        indexPath = configuration.getIndexPath();
        channelWorker = workerProvider.getChannelWorker(indexPath + SPEC_INDEX_FILE);

        File idx = new File(indexPath + SPEC_INDEX_FILE);
        File bak = new File(indexPath + SPEC_INDEX_BACKUP_FILE);
        if (idx.exists() && bak.exists() && idx.length() != bak.length()) {
            Helper.copyFile(bak.getAbsolutePath(), idx.getAbsolutePath());
        }
    }

    @Override
    public void store(final SpecIndexFrame indexFrame) {
        if (indexFrame.isStored()) {
            return;
        }
        indexFrame.dropPreviousFrames(channelWorker, specIndexLock);
        channelWorker.processOutputChannel(new ChannelHandler() {
            @Override
            public void handle(SeekableByteChannel channel) throws Exception {
                indexFrame.store(channel, specIndexLock);
            }
        }, true);
    }

    @Override
    public SortedMap<Number, IntRange> loadFromRegistry(char...c) {
        SortedMap<Number, IntRange> fileNums = new TreeMap<>();
        if (c.length > 0) {
            Map<Number, IntRange> frameInfoMap = specRegistry.getFrameInfosByChar(c[0]);
            if (frameInfoMap == null) {
                return fileNums;
            }
            for (Map.Entry<Number, IntRange> entry : frameInfoMap.entrySet()) {
                fileNums.put(entry.getKey(), entry.getValue());
            }
        }
        return fileNums;
    }

    @Override
    public SpecEntry loadFromIndex(char... c) {
        SortedMap<Number, IntRange> frameInfoMap = loadFromRegistry(c[0]);
        if (frameInfoMap == null) {
            return null;
        }
        return loadFromIndexByRange(frameInfoMap, c);
    }

    @Override
    public SpecEntry loadFromIndexByRange(final Map<Number, IntRange> frameInfoMap, char... c) {
        final SpecEntry specEntry = specEntryFactory.createSpecEntry(c[0]);
        channelWorker.processInputChannel(new ChannelHandler() {
            @Override
            public void handle(SeekableByteChannel channel) throws Exception {
                specEntry.load(channel, specIndexLock, frameInfoMap);
            }
        });
        return specEntry;
    }

    @Override
    public boolean deleteFromIndex(Number fileNum) {
        specIndexLock.writeLock().lock();
        try {
            Map<Number, IntRange> frameInfoMap = specRegistry.removeFrameInfosByFile(fileNum);
            for (Range frameInfo : frameInfoMap.values()) {
                if (frameInfo == null) {
                    continue;
                }
                Number startNumber = frameInfo.getStart();
                long start = startNumber.longValue();
                Number stopNumber = frameInfo.getStop();
                long stop = stopNumber.longValue();
                channelWorker.cut(start, stop, specIndexLock);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error while deleting from \"" + SPEC_INDEX_FILE + "\"  -> ", e);
        } finally {
            specIndexLock.writeLock().unlock();
        }
        return false;
    }

    public boolean backup() {
        boolean result = false;
        specIndexLock.writeLock().lock();
        try {
            boolean copied = Helper.copyFile(indexPath + SPEC_INDEX_FILE, indexPath + SPEC_INDEX_BACKUP_FILE);
            if (copied) {
                specRegistry.createDump();
                result = true;
            }
        } finally {
            specIndexLock.writeLock().unlock();
        }
        return result;
    }
}
