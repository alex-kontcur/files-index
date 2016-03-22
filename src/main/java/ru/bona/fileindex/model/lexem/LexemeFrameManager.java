package ru.bona.fileindex.model.lexem;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.channelworker.ChannelWorkerProvider;
import ru.bona.fileindex.model.FrameManager;
import ru.bona.fileindex.model.fileparser.ChannelHandler;
import ru.bona.fileindex.model.lexem.factory.LexemEntryFactory;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.model.range.Range;
import ru.bona.fileindex.utils.Helper;

import java.io.File;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * LexemeFrameManager
 *
 * @author Kontsur Alex (bona)
 * @since 22.09.14
 */
public class LexemeFrameManager implements FrameManager<LexemEntry, LexemeIndexFrame> {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final String LEXEM_INDEX_FILE = "lexem.idx";
    private static final String LEXEM_INDEX_BACKUP_FILE = "lexem.idx.bak";

    private static final Logger logger = LoggerFactory.getLogger(LexemeFrameManager.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    // Будет использоваться для блокирования параллельных операций на индексном файле
    // например для вырезания сегмента из середины файла. Но в случае с лексемами
    // например при удалении файла нужно не только вырезать соответствующий фрэйм из
    // индексного файла, но и дописать новую версию набора LexemRange для оставшихся
    // файлов в конец индексного файла (не забывая конечно и о реестре, но там просто действия в памяти)
    //
    // Файл реестра будет бэкапиться
    // на диск по расписанию, создавая при этом актуальную на тот момент копию индексного файла.
    private ReadWriteLock lexemeIndexLock;

    private LexemEntryFactory entryFactory;
    private LexemeRegistry lexemeRegistry;
    private ChannelWorker channelWorker;
    private String indexPath;

    /*===========================================[ CONSTRUCTORS ]=================*/

    @Inject
    protected void init(LexemeRegistry lexemeRegistry, LexemEntryFactory entryFactory,
                        ChannelWorkerProvider workerProvider, Configuration configuration) {
        this.lexemeRegistry = lexemeRegistry;
        this.entryFactory = entryFactory;

        lexemeIndexLock = new ReentrantReadWriteLock();

        indexPath = configuration.getIndexPath();
        channelWorker = workerProvider.getChannelWorker(indexPath + LEXEM_INDEX_FILE);

        File idx = new File(indexPath + LEXEM_INDEX_FILE);
        File bak = new File(indexPath + LEXEM_INDEX_BACKUP_FILE);
        if (idx.exists() && bak.exists() && idx.length() != bak.length()) {
            Helper.copyFile(bak.getAbsolutePath(), idx.getAbsolutePath());
        }
    }

    @Override
    public void store(final LexemeIndexFrame indexFrame) {
        if (indexFrame.isStored()) {
            return;
        }
        indexFrame.dropPreviousFrames(channelWorker, lexemeIndexLock);
        channelWorker.processOutputChannel(new ChannelHandler() {
            @Override
            public void handle(SeekableByteChannel channel) throws Exception {
                indexFrame.store(channel, lexemeIndexLock);
            }
        }, true);
    }

    public Map<String, Map<Number, IntRange>> loadFromRegistryFuzzy(char... c) {
        String lexeme = String.valueOf(c);
        return lexemeRegistry.getFuzzyLexemsMap(lexeme);
    }

    @Override
    public SortedMap<Number, IntRange> loadFromRegistry(char... c) {
        SortedMap<Number, IntRange> fileNums = new TreeMap<>();
        if (c.length > 0) {
            String lexeme = String.valueOf(c);
            Map<Number, IntRange> frameInfoMap = lexemeRegistry.getFrameInfosByLexeme(lexeme);
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
    public LexemEntry loadFromIndex(char...charArray) {
        String lexeme = String.valueOf(charArray);
        Map<Number, IntRange> frameInfoMap = lexemeRegistry.getFrameInfosByLexeme(lexeme);
        if (frameInfoMap == null) {
            //No lexem found in registry
            return null;
        }
        return loadFromIndexByRange(frameInfoMap, charArray);
    }

    @Override
    public LexemEntry loadFromIndexByRange(final Map<Number, IntRange> frameInfoMap, char... c) {
        String lexeme = String.valueOf(c);
        final LexemEntry lexemEntry = entryFactory.createLexemEntry(lexeme);
        channelWorker.processInputChannel(new ChannelHandler() {
            @Override
            public void handle(SeekableByteChannel channel) throws Exception {
                lexemEntry.load(channel, lexemeIndexLock, frameInfoMap);
            }
        });
        return lexemEntry;
    }

    @Override
    public boolean deleteFromIndex(Number fileNum) {
        try {
            Map<Number, IntRange> frameInfoMap = lexemeRegistry.removeFrameInfosForFile(fileNum);
            for (Range range : frameInfoMap.values()) {
                long start = range.getStart().longValue();
                long stop = range.getStop().longValue();
                channelWorker.cut(start, stop, lexemeIndexLock);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error while deleting from \"" + LEXEM_INDEX_FILE + "\"  -> ", e);
        }
        return false;
    }

    public boolean backup() {
        boolean result = false;
        lexemeIndexLock.writeLock().lock();
        try {
            boolean copied = Helper.copyFile(indexPath + LEXEM_INDEX_FILE, indexPath + LEXEM_INDEX_BACKUP_FILE);
            if (copied) {
                lexemeRegistry.createDump();
                result = true;
            }
        } finally {
            lexemeIndexLock.writeLock().unlock();
        }
        return result;
    }

}
