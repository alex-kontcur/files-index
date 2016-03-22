package ru.bona.fileindex.search.searcher;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.channelworker.ChannelWorkerProvider;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.filesholder.FilesHolder;
import ru.bona.fileindex.model.BufferInfo;
import ru.bona.fileindex.model.fileparser.ChannelHandler;
import ru.bona.fileindex.model.fileparser.FileParser;
import ru.bona.fileindex.model.lexem.LexemEntry;
import ru.bona.fileindex.model.lexem.LexemeFrameManager;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.model.range.Range;
import ru.bona.fileindex.model.spec.SpecEntry;
import ru.bona.fileindex.model.spec.SpecFrameManager;
import ru.bona.fileindex.model.spec.synctree.SyncTree;
import ru.bona.fileindex.search.CharsPosition;
import ru.bona.fileindex.search.FileSearchInfo;
import ru.bona.fileindex.search.SearchRecord;
import ru.bona.fileindex.utils.Helper;
import ru.bona.fileindex.utils.SplittedRunnable;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IndexSearcher
 *
 * @author Kontsur Alex (bona)
 * @since 28.09.14
 */
public class IndexSearcher {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(IndexSearcher.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private ChannelWorkerProvider channelWorkerProvider;
    private LexemeFrameManager lexemeFrameManager;
    private SpecFrameManager specFrameManager;
    private FilesHolder filesHolder;
    private int tokenSize;

    /*===========================================[ CONSTRUCTORS ]=================*/

    @Inject
    protected void init(LexemeFrameManager lexemeFrameManager, SpecFrameManager specFrameManager,
                        FilesHolder filesHolder, Configuration configuration, ChannelWorkerProvider channelWorkerProvider) {
        this.channelWorkerProvider = channelWorkerProvider;
        this.lexemeFrameManager = lexemeFrameManager;
        this.specFrameManager = specFrameManager;
        this.filesHolder = filesHolder;

        tokenSize = configuration.getTokenSize();
    }

    /*===========================================[ CLASS METHODS ]================*/

    public Collection<FileSearchInfo> search(String source, SearchRecord searchRecord) {
        Collection<FileSearchInfo> searchInfos = new HashSet<>();

        List<CharsPosition> specChars = searchRecord.getSpecChars();
        List<CharsPosition> lexems = searchRecord.getLexems();
        Set<File> matchedFiles = new HashSet<>();
        if (lexems.size() <= tokenSize) {
            // одна лексема с одним или двумя символами - нужно найти все начинающиеся на них лексемы
            // и собрать по ним пересекающиеся файлы
            CharsPosition charsPosition = lexems.get(0);
            Map<String, Map<Number, IntRange>> fuzzyMap = lexemeFrameManager.loadFromRegistryFuzzy(charsPosition.getChars());
            if (specChars.isEmpty()) {
                processSmallLexeme(searchInfos, fuzzyMap);
            } else {
                processSmallLexemeWithSpec(searchInfos, fuzzyMap, source, specChars);
            }
        } else {
            Map<CharsPosition, Map<Number, IntRange>> targetFilesFrames = new ConcurrentHashMap<>();
            collectFileNums(targetFilesFrames, source, specChars, lexems);

            // 1. Сначала находим пересекающиеся файлы
            Set<Number> intersectedFiles = new HashSet<>();
            Map<CharsPosition, Map<Number, IntRange>> intersectedChars = new HashMap<>();
            findIntersected(targetFilesFrames, intersectedChars, intersectedFiles);

            if (targetFilesFrames.keySet().size() != intersectedChars.keySet().size()) {
                // Это значит, что нет файла, где встречаются все лексемы и спец символы, а значит
                // поиск можно остановить
                return searchInfos;
            }

            // 2. Для всех пересекающихся уже поднимаем из индекса и анализируем смещения и размерности
            //    лексем и позиции спец символов

            Map<CharsPosition, Map<Number, SortedSet<Range>>> charRanges = new HashMap<>();
            Map<CharsPosition, Map<Number, Range>> indexRanges = new HashMap<>();
            Map<CharsPosition, Map<Number, SyncTree>> charTrees = new HashMap<>();
            collectRealIntersectedPositions(charRanges, charTrees, indexRanges, intersectedChars);
            List<AnalyzingEntry> analyzingEntries = new ArrayList<>();
            prepareAnalyzingEntries(source, intersectedFiles, analyzingEntries, indexRanges, intersectedChars, charRanges, charTrees);
            processCandidates(analyzingEntries, source, searchInfos);
        }
        return searchInfos;
    }

    private void processSmallLexemeWithSpec(final Collection<FileSearchInfo> matchedFileInfos,
                                            Map<String, Map<Number, IntRange>> fuzzyMap,
                                            final String source, final List<CharsPosition> specChars) {

        Helper.splitThread(fuzzyMap.entrySet(), new SplittedRunnable<Map.Entry<String, Map<Number, IntRange>>>() {
            @Override
            public void run(Map.Entry<String, Map<Number, IntRange>> element) {
                String lexem = element.getKey();
                Map<Number, IntRange> rangeMap = element.getValue();
                if (rangeMap == null) {
                    return;
                }
                LexemEntry lexemEntry = lexemeFrameManager.loadFromIndexByRange(rangeMap, lexem.toCharArray());
                for (Number fileNum : rangeMap.keySet()) {
                    SortedSet<Range> ranges = lexemEntry.getRanges(fileNum);
                    if (ranges == null) {
                        continue;
                    }
                    processCandidate(source, matchedFileInfos, fileNum, ranges, specChars.size());
                }
            }
        });
    }

    private void processSmallLexeme(Collection<FileSearchInfo> matchedFileInfos, Map<String, Map<Number, IntRange>> fuzzyMap) {
        for (Map.Entry<String, Map<Number, IntRange>> entry : fuzzyMap.entrySet()) {
            String source = entry.getKey();
            Map<Number, IntRange> rangeMap = entry.getValue();
            if (rangeMap == null) {
                continue;
            }
            LexemEntry lexemEntry = lexemeFrameManager.loadFromIndex(source.toCharArray());
            Map<File, SortedSet<Range>> fileMap = new HashMap<>();
            for (Number fileNum : rangeMap.keySet()) {
                FileInfo fileInfo = filesHolder.getFileInfo(fileNum);
                if (fileInfo != null) {
                    SortedSet<Range> ranges = lexemEntry.getRanges(fileNum);
                    if (ranges == null) {
                        continue;
                    }
                    fileMap.put(fileInfo.getFile(), ranges);
                }
            }
            matchedFileInfos.add(new FileSearchInfo(source, fileMap));
        }
    }

    private void processCandidates(List<AnalyzingEntry> analyzingEntries, final String source,
                                   final Collection<FileSearchInfo> matchedFileInfos) {
        List<AnalyzingEntry> candidates = new ArrayList<>();
        for (AnalyzingEntry entry : analyzingEntries) {
            if (entry.getState().equals(MatchState.MATCHED)) {
                FileInfo fileInfo = filesHolder.getFileInfo(entry.getFileNum());
                SortedSet<Range> multiRanges = entry.getLexemesStartStopRanges();
                Map<File, SortedSet<Range>> fileMap = new HashMap<>();
                fileMap.put(fileInfo.getFile(), multiRanges);
                matchedFileInfos.add(new FileSearchInfo(source, fileMap));

            } else if (entry.getState().equals(MatchState.MATCH_CANDIDATE)) {
                candidates.add(entry);
            }
        }
        Helper.splitThread(candidates, new SplittedRunnable<AnalyzingEntry>() {
            @Override
            public void run(AnalyzingEntry element) {
                Number fileNum = element.getFileNum();
                SortedSet<Range> multiRanges = element.getLexemesStartStopRanges();
                processCandidate(source, matchedFileInfos, fileNum, multiRanges, element.specsCount());
            }
        });
    }

    private void processCandidate(String source, Collection<FileSearchInfo> matchedFileInfos, Number fileNum,
                                  SortedSet<Range> multiRanges, int specCount) {

        FileInfo fileInfo = filesHolder.getFileInfo(fileNum);
        if (fileInfo != null) {
            String encoding = fileInfo.getEncoding();
            CharsetDecoder decoder = Helper.prepareDecoder(encoding);

            SortedSet<BufferInfo> bufferInfos = new TreeSet<>();
            fillBufferInfos(bufferInfos, multiRanges, fileInfo, decoder, specCount);

            List<Range> multiList = new ArrayList<>(multiRanges);
            List<BufferInfo> buffersList = new ArrayList<>(bufferInfos);
            SortedSet<Range> matchedSet = new TreeSet<>();
            for (int i = 0; i < buffersList.size(); i++) {
                BufferInfo bufferInfo = buffersList.get(i);
                if (bufferInfo.getSourcePart().contains(source)) {
                    matchedSet.add(multiList.get(i));
                }
            }

            if (!matchedSet.isEmpty()) {
                Map<File, SortedSet<Range>> fileMap = new HashMap<>();
                fileMap.put(fileInfo.getFile(), matchedSet);
                matchedFileInfos.add(new FileSearchInfo(source, fileMap));
            }
        }
    }

    private void fillBufferInfos(final SortedSet<BufferInfo> bufferInfos, SortedSet<Range> multiRanges, FileInfo fileInfo,
                                 final CharsetDecoder decoder, final int specCount) {
        if (multiRanges.isEmpty()) {
            return;
        }
        ChannelWorker channelWorker = channelWorkerProvider.getChannelWorker(fileInfo.getFullPath());
        for (final Range multiRange : multiRanges) {
            channelWorker.processInputChannel(new ChannelHandler() {
                @Override
                public void handle(SeekableByteChannel channel) throws Exception {
                    Long start = multiRange.getStart().longValue();
                    Long stop = multiRange.getStop().longValue();

                    int capacity = (int) (stop - start) + specCount;
                    ByteBuffer data = ByteBuffer.allocate(capacity * 2);
                    CharBuffer chBuffer = CharBuffer.allocate(capacity * 2);

                    channel.position(start - specCount);

                    BufferInfo bufferInfo = FileParser.extractChars(channel, decoder, data, chBuffer);
                    bufferInfos.add(bufferInfo);
                }
            });
        }
    }

    private void prepareAnalyzingEntries(String source, Set<Number> intersectedFiles,
                                         List<AnalyzingEntry> analyzingEntries,
                                         Map<CharsPosition, Map<Number, Range>> indexRanges,
                                         Map<CharsPosition, Map<Number, IntRange>> intersectedChars,
                                         Map<CharsPosition, Map<Number, SortedSet<Range>>> charRanges,
                                         Map<CharsPosition, Map<Number, SyncTree>> charTrees) {
        for (Number fileNum : intersectedFiles) {
            SortedSet<CharsPosition> charsPositions = new TreeSet<>();
            for (Map.Entry<CharsPosition, Map<Number, IntRange>> entry : intersectedChars.entrySet()) {
                if (entry.getValue().keySet().contains(fileNum)) {
                    charsPositions.add(entry.getKey());
                }
            }
            Map<CharsPosition, Range> indexRanges2 = new HashMap<>();
            for (Map.Entry<CharsPosition, Map<Number, Range>> entry : indexRanges.entrySet()) {
                CharsPosition charsPosition = entry.getKey();
                indexRanges2.put(charsPosition, entry.getValue().get(fileNum));
            }

            Map<CharsPosition, SortedSet<Range>> ranges = new HashMap<>();
            for (Map.Entry<CharsPosition, Map<Number, SortedSet<Range>>> entry : charRanges.entrySet()) {
                CharsPosition charsPosition = entry.getKey();
                ranges.put(charsPosition, entry.getValue().get(fileNum));
            }
            Map<CharsPosition, SyncTree> trees = new HashMap<>();
            for (Map.Entry<CharsPosition, Map<Number, SyncTree>> entry : charTrees.entrySet()) {
                CharsPosition charsPosition = entry.getKey();
                trees.put(charsPosition, entry.getValue().get(fileNum));
            }
            AnalyzingEntry analyzingEntry = new AnalyzingEntry(source, fileNum, charsPositions, ranges, indexRanges2, trees, tokenSize);
            analyzingEntries.add(analyzingEntry);
        }
    }

    private void collectRealIntersectedPositions(Map<CharsPosition, Map<Number, SortedSet<Range>>> charRanges,
                                                 Map<CharsPosition, Map<Number, SyncTree>> charTrees,
                                                 Map<CharsPosition, Map<Number, Range>> indexRanges,
                                                 Map<CharsPosition, Map<Number, IntRange>> intersectedChars) {
        for (Map.Entry<CharsPosition, Map<Number, IntRange>> entry : intersectedChars.entrySet()) {
            CharsPosition charsPosition = entry.getKey();
            Map<Number, IntRange> rangeMap = entry.getValue();
            if (charsPosition.isLexem()) {
                LexemEntry lexemEntry = lexemeFrameManager.loadFromIndex(charsPosition.getChars());
                if (lexemEntry == null) {
                    continue;
                }

                Map<Number, Range> numberRangeMap = indexRanges.get(charsPosition);
                if (numberRangeMap == null) {
                    numberRangeMap = new HashMap<>();
                    indexRanges.put(charsPosition, numberRangeMap);
                }
                for (Number number : rangeMap.keySet()) {
                    numberRangeMap.put(number, rangeMap.get(number));
                }

                Map<Number, SortedSet<Range>> fileRanges = charRanges.get(charsPosition);
                if (fileRanges == null) {
                    fileRanges = new HashMap<>();
                    charRanges.put(charsPosition, fileRanges);
                }
                for (Number number : rangeMap.keySet()) {
                    fileRanges.put(number, lexemEntry.getRanges(number));
                }

            } else {
                SpecEntry specEntry = specFrameManager.loadFromIndex(charsPosition.getChars());
                if (specEntry == null) {
                    continue;
                }

                Map<Number, SyncTree> treeMap = charTrees.get(charsPosition);
                if (treeMap == null) {
                    treeMap = new HashMap<>();
                    charTrees.put(charsPosition, treeMap);
                }
                for (Number number : treeMap.keySet()) {
                    treeMap.put(number, specEntry.getTree(number));
                }
            }
        }
    }

    private void findIntersected(Map<CharsPosition, Map<Number, IntRange>> targetFilesFrames,
                                 Map<CharsPosition, Map<Number, IntRange>> intersectedChars,
                                 Set<Number> intersected) {
        Map<CharsPosition, Set<Number>> charsSets = new HashMap<>();
        for (Map.Entry<CharsPosition, Map<Number, IntRange>> entry : targetFilesFrames.entrySet()) {
            charsSets.put(entry.getKey(), entry.getValue().keySet());
        }
        List<CharsPosition> chars = new ArrayList<>(charsSets.keySet());

        if (charsSets.size() >= tokenSize - 1) {
            CharsPosition charsPosition1 = chars.get(0);
            CharsPosition charsPosition2 = chars.get(1);
            Sets.SetView<Number> intersection = Sets.intersection(charsSets.get(charsPosition2), charsSets.get(charsPosition1));
            intersection.copyInto(intersected);
            for (int i = 2; i < chars.size(); i++) {
                CharsPosition charsPositionNext = chars.get(i);
                intersection = Sets.intersection(charsSets.get(charsPositionNext), intersected);
                intersection.copyInto(intersected);
            }
        }

        for (Number fileNum : intersected) {
            for (Map.Entry<CharsPosition, Map<Number, IntRange>> entry : targetFilesFrames.entrySet()) {
                Map<Number, IntRange> rangeMap = entry.getValue();
                if (rangeMap.keySet().contains(fileNum)) {
                    intersectedChars.put(entry.getKey(), rangeMap);
                }
            }
        }
    }

    private void collectFileNums(final Map<CharsPosition, Map<Number, IntRange>> targetFilesFrames, String source,
                                 List<CharsPosition> specChars, List<CharsPosition> lexems) {

        List<CharsPosition> filtered = new ArrayList<>();
        for (CharsPosition lexem : lexems) {
            if (lexem.getChars().length < tokenSize) {
                continue;
            }
            filtered.add(lexem);
        }
        logger.info("Collecting of fileNums for {} started", source);
        Helper.splitThread(specChars, new SplittedRunnable<CharsPosition>() {
            @Override
            public void run(CharsPosition element) {
                Map<Number, IntRange> specFilesMap = specFrameManager.loadFromRegistry(element.getChars());
                if (!specFilesMap.isEmpty()) {
                    targetFilesFrames.put(element, specFilesMap);
                }
            }
        });
        Helper.splitThread(filtered, new SplittedRunnable<CharsPosition>() {
            @Override
            public void run(CharsPosition element) {
                SortedMap<Number, IntRange> lexemsFilesMap = lexemeFrameManager.loadFromRegistry(element.getChars());
                if (!lexemsFilesMap.isEmpty()) {
                    targetFilesFrames.put(element, lexemsFilesMap);
                }
            }
        });
        logger.info("Collecting of fileNums for {} stopped", source);
    }


}
