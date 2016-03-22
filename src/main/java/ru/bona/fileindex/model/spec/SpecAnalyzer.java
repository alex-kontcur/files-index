package ru.bona.fileindex.model.spec;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.exceptions.StoppedException;
import ru.bona.fileindex.model.BufferInfo;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.model.SourceAnalyzer;
import ru.bona.fileindex.model.SourceParser;
import ru.bona.fileindex.model.fileparser.CharBufferAction;
import ru.bona.fileindex.model.spec.factory.SpecIndexFrameFactory;
import ru.bona.fileindex.model.spec.synctree.SyncTree;
import ru.bona.fileindex.search.CharsPosition;
import ru.bona.fileindex.utils.Helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SpecAnalyzer
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */
public class SpecAnalyzer implements SourceAnalyzer {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(SpecAnalyzer.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private SpecIndexFrameFactory frameFactory;
    private SpecFrameManager specFrameManager;
    private SourceParser sourceParser;

    /*===========================================[ CLASS METHODS ]================*/

    @Inject
    protected void init(SpecFrameManager specFrameManager, SpecIndexFrameFactory frameFactory, SourceParser sourceParser) {
        this.specFrameManager = specFrameManager;
        this.frameFactory = frameFactory;
        this.sourceParser = sourceParser;
    }

    @Override
    public void analyze(FileInfo fileInfo, AtomicBoolean terminate) {
        SpecIndexFrame indexFrame = prepareIndexFrame(fileInfo, terminate);
        if (terminate.get()) {
            return;
        }
        specFrameManager.store(indexFrame);
    }

    private SpecIndexFrame prepareIndexFrame(FileInfo fileInfo, final AtomicBoolean terminate) {
        final Map<Character, SyncTree> treeMap = new ConcurrentHashMap<>();
        final String fileName = fileInfo.getFullPath();
        final AtomicLong utfDelta = new AtomicLong(0);
        sourceParser.processSourceByPieces(fileInfo, new CharBufferAction() {
            @Override
            public void processBuffer(BufferInfo bufferInfo, FileInfo fileInfo) {
                if (terminate.get()) {
                    String msg = "Spec analyzer terminated";
                    logger.info(msg);
                    throw new StoppedException(msg);
                }
                fillTree(treeMap, bufferInfo, fileName, fileInfo.getFileSize(), utfDelta);
            }
        });
        return frameFactory.createSpecIndexFrame(fileInfo.getNum(), treeMap);
    }

    private static void fillTree(Map<Character, SyncTree> treeMap, BufferInfo bufferInfo,
                                 String fileName, long fileSize, AtomicLong utfDelta) {

        String source = bufferInfo.getSourcePart();
        long start = bufferInfo.getStart();
        long stop = bufferInfo.getStop();

        for (int i = 0; i < source.length(); i++) {
            Character c = source.charAt(i);
            if (Helper.isTwoBytesChar(c)) {
                utfDelta.incrementAndGet();
            }
            if (SpecRegistry.isSpecChar(c)) {

                if (i + 1 < source.length()) {
                    Character cNext = source.charAt(i + 1);
                    if (!SpecRegistry.isSpecChar(cNext) || SpecRegistry.isDelimeterChar(cNext)) {
                        // Индексируем только спец символы, за которыми идут другие спец символы,
                        // если далее идут разделители, не индексируем
                        continue;
                    }
                }

                SyncTree syncTree = treeMap.get(c);
                if (syncTree == null) {
                    syncTree = Helper.makeSyncTree(fileSize);
                    treeMap.put(c, syncTree);
                }

                syncTree.addPosition(Helper.adaptValueForSize(start, i, fileSize, utfDelta));
                utfDelta.set(0);
            }
        }
    }

    @Override
    public List<CharsPosition> getCharsPositions(String target) {
        List<CharsPosition> positions = new ArrayList<>();
        for (int i = 0; i < target.length(); i++) {
            Character c = target.charAt(i);
            if (SpecRegistry.isSpecChar(c)) {
                if (SpecRegistry.isDelimeterChar(c)) {
                    // Индексируем только спец символы, за которыми идут другие спец символы,
                    // если далее идут разделители, не индексируем
                    continue;
                }
                positions.add(new CharsPosition(i, c));
            }
        }
        return positions;
    }

}
