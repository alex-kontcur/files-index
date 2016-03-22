package ru.bona.fileindex.model.lexem;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.exceptions.StoppedException;
import ru.bona.fileindex.model.BufferInfo;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.model.SourceAnalyzer;
import ru.bona.fileindex.model.SourceParser;
import ru.bona.fileindex.model.fileparser.CharBufferAction;
import ru.bona.fileindex.model.lexem.factory.LexemeIndexFrameFactory;
import ru.bona.fileindex.model.range.Range;
import ru.bona.fileindex.model.spec.SpecRegistry;
import ru.bona.fileindex.search.CharsPosition;
import ru.bona.fileindex.utils.Helper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LexemeAnalyzer
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */
public class LexemeAnalyzer implements SourceAnalyzer {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(LexemeAnalyzer.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private int tokenSize;
    private SourceParser sourceParser;
    private LexemeFrameManager frameManager;
    private LexemeIndexFrameFactory frameFactory;

    /*===========================================[ CLASS METHODS ]================*/

    @Inject
    protected void init(LexemeFrameManager frameManager, LexemeIndexFrameFactory frameFactory,
                     SourceParser sourceParser, Configuration configuration) {
        this.frameManager = frameManager;
        this.frameFactory = frameFactory;
        this.sourceParser = sourceParser;

        tokenSize = configuration.getTokenSize();
    }

    @Override
    public void analyze(FileInfo fileInfo, AtomicBoolean terminate) {
        List<LexemeIndexFrame> frameList = prepareIndexFrame(fileInfo, terminate);
        for (LexemeIndexFrame indexFrame : frameList) {
            if (terminate.get()) {
                break;
            }
            frameManager.store(indexFrame);
        }
    }

    private static class StringHolder {

        private String previous = "";

        private String getPrevious() {
            return previous;
        }

        private void setPrevious(String previous) {
            this.previous = previous;
        }
    }

    private List<LexemeIndexFrame> prepareIndexFrame(FileInfo fileInfo, final AtomicBoolean terminate) {
        final StringHolder remaining = new StringHolder();
        String fileName = fileInfo.getFullPath();
        final long fileSize = fileInfo.getFileSize();
        final Map<String, SortedSet<Range>> lexemeMap = new ConcurrentHashMap<>();
        final TokenCollector collector = new TokenCollector() {
            @Override
            public void collect(String token, long start, long stop, long fileSize) {
                SortedSet<Range> infos = lexemeMap.get(token);
                if (infos == null) {
                    infos = new TreeSet<>();
                    lexemeMap.put(token, infos);
                }
                Range lexemRange = Helper.makeRange(fileSize);
                lexemRange.setStart(start);
                lexemRange.setStop(stop);
                infos.add(lexemRange);
            }
        };
        sourceParser.processSourceByPieces(fileInfo, new CharBufferAction() {
            @Override
            public void processBuffer(BufferInfo bufferInfo, FileInfo fileInfo) {
                if (terminate.get()) {
                    String msg = "Lexeme analyzer terminated";
                    logger.info(msg);
                    throw new StoppedException(msg);
                }
                String source = remaining.getPrevious() + bufferInfo.getSourcePart();
                String filtered = SpecRegistry.cleanSource(source);
                remaining.setPrevious(pickTokens(collector, filtered, bufferInfo.getStart(), bufferInfo.isLast(), fileSize, 2));
            }
        });

        List<LexemeIndexFrame> frameList = new ArrayList<>();
        for (Map.Entry<String, SortedSet<Range>> entry : lexemeMap.entrySet()) {
            String lexeme = entry.getKey();
            SortedSet<Range> lexemRanges = entry.getValue();
            frameList.add(frameFactory.createLexemeIndexFrame(lexeme, fileInfo.getNum(), lexemRanges));
        }
        return frameList;
    }

    @Override
    public List<CharsPosition> getCharsPositions(String target) {
        final List<CharsPosition> charsPositions = new ArrayList<>();
        TokenCollector collector = new TokenCollector() {
            @Override
            public void collect(String token, long start, long stop, long fileSize) {
                CharsPosition charsPosition = new CharsPosition((int) start, (int) stop, token.toCharArray());
                charsPositions.add(charsPosition);
            }
        };
        String source = SpecRegistry.cleanSource(target);
        pickTokens(collector, source, 0, true, target.length(), 1);
        return charsPositions;
    }

    public String pickTokens(TokenCollector collector, String source,
                             long start, boolean last, long fileSize, int minTokenSize) {
        String remaining = "";
        int len = source.length();
        for (int i = 0; i < len; i++) {
            if (source.charAt(i) == ' ') {
                continue;
            }
            if (i <= len - tokenSize) {
                String next = source.substring(i, i + tokenSize);

                int h = -1;
                int k = -1;
                while (next.contains(" ") || next.length() < tokenSize) {
                    while (i + tokenSize + k < len && source.charAt(i + tokenSize + k) == ' ') {
                        k++;
                    }
                    next = i + tokenSize + k == len ? source.substring(i, i + tokenSize + k) : source.substring(i, i + tokenSize + k + 1);
                    next = next.replace(" ", "");
                    h = k;
                    if (i + tokenSize + k >= len - 1) {
                        break;
                    }
                    k += 1;
                }
                if (next.length() == tokenSize) {
                    collector.collect(next, start + i, start + i + tokenSize + h, fileSize);
                } else if (next.length() > minTokenSize - 1) {
                    if (last) {
                        collector.collect(next, i, len - 1, fileSize);
                    } else {
                        remaining = next;
                    }
                }
            } else {
                String lastLexem = source.substring(i, len).trim();
                if (lastLexem.length() > minTokenSize - 1) {
                    if (last) {
                        collector.collect(lastLexem, i, len - 1, fileSize);
                    } else {
                        remaining = lastLexem;
                    }
                }
            }
        }
        return remaining;
    }

    public interface TokenCollector {
        void collect(String token, long start, long stop, long fileSize);
    }

}
