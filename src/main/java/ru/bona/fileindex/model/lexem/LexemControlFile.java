package ru.bona.fileindex.model.lexem;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.model.ControlFile;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.utils.Helper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LexemControlFile
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */
public class LexemControlFile extends ControlFile {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(LexemControlFile.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private int tokenSize;
    private Map<String, Map<Number, IntRange>> lexemsMap;

    /*===========================================[ CONSTRUCTORS ]=================*/

    @Inject
    protected void init(Configuration configuration) {
        lexemsMap = new ConcurrentHashMap<>();
        tokenSize = configuration.getTokenSize();
    }

    /*===========================================[ CLASS METHODS ]================*/

    public Map<String, Map<Number, IntRange>> getFuzzyLexemsMap(String lexeme) {
        Map<String, Map<Number, IntRange>> fuzzy = new ConcurrentHashMap<>();
        if (lexeme.length() == tokenSize) {
            Map<Number, IntRange> rangeMap = getRangeInfosByLexeme(lexeme);
            if (rangeMap != null) {
                fuzzy.put(lexeme, rangeMap);
            }
            return fuzzy;
        }

        for (Map.Entry<String, Map<Number, IntRange>> entry : lexemsMap.entrySet()) {
            String lexemeKey = entry.getKey();
            if (lexemeKey.startsWith(lexeme)) {
                Map<Number, IntRange> rangeMap = fuzzy.get(lexemeKey);
                if (rangeMap == null) {
                    rangeMap = new HashMap<>();
                    fuzzy.put(lexemeKey, rangeMap);
                }
                rangeMap.putAll(entry.getValue());
            }
        }
        return fuzzy;
    }

    public Map<Number, IntRange> getRangeInfosByLexeme(String lemexe) {
        return lexemsMap.get(lemexe);
    }

    public void put(String lemexe, Map<Number, IntRange> setMap) {
        Map<Number, IntRange> rangeMap = getRangeInfosByLexeme(lemexe);
        if (rangeMap == null) {
            rangeMap = new ConcurrentHashMap<>();
            lexemsMap.put(lemexe, rangeMap);
        }

        for (Map.Entry<Number, IntRange> entry : setMap.entrySet()) {
            rangeMap.put(entry.getKey(), entry.getValue());
        }
    }

    public void removeFrameInfosForLexeme(String lexeme, Number fileNum) {
        Map<Number, IntRange> numberRangeMap = lexemsMap.get(lexeme);
        if (numberRangeMap == null || numberRangeMap.isEmpty()) {
            return;
        }
        numberRangeMap.remove(fileNum);
    }

    public Map<Number, IntRange> removeFrameInfosForFile(Number fileNum) {
        Map<Number, IntRange> filesMap = new HashMap<>();
        for (Map.Entry<String, Map<Number, IntRange>> entry : lexemsMap.entrySet()) {
            Map<Number, IntRange> infoMap = entry.getValue();
            filesMap.put(fileNum, infoMap.remove(fileNum));
        }
        return filesMap;
    }

    @Override
    public void writeExternal(SeekableByteChannel channel) throws IOException {
        long filesCount = filesHolder.getFilesCount();
        try {
            Set<Map.Entry<String, Map<Number, IntRange>>> entrySet = lexemsMap.entrySet();
            for (Map.Entry<String, Map<Number, IntRange>> entry : entrySet) {

                Map<Number, IntRange> pairMap = entry.getValue();
                if (pairMap.isEmpty()) {
                    continue;
                }

                String lexeme = entry.getKey();
                char[] chars = lexeme.toCharArray();
                ByteBuffer valueBuffer = ByteBuffer.allocate(tokenSize * 2);
                for (char c : chars) {
                    valueBuffer.putChar(c);
                }
                int len = chars.length;
                while (len < tokenSize) {
                    valueBuffer.putChar('\u0000');
                    len++;
                }
                saveBuffer(channel, valueBuffer);

                int filesCountForLexem = pairMap.entrySet().size();
                valueBuffer = Helper.getByteBuffer(filesCount, filesCountForLexem);
                saveBuffer(channel, valueBuffer);

                saveFileFrameMap(channel, pairMap);
            }
        } catch (Throwable e) {
            logger.error("Error while  ->", e);
        }
    }

    @Override
    public void readExternal(SeekableByteChannel channel) throws IOException {
        lexemsMap.clear();

        channel.position(0);
        while (channel.position() < channel.size()) {

            ByteBuffer buffer = ByteBuffer.allocate(tokenSize * 2);
            channel.read(buffer);
            buffer.flip();

            CharBuffer charBuffer = buffer.asCharBuffer();
            String lexeme = charBuffer.toString().trim();

            Number filesCount = Helper.readNumFromChannel(filesHolder.getFilesCount(), channel);

            Map<Number, IntRange> framesMap = new HashMap<>();
            fillRangeMap(framesMap, channel, filesCount);

            lexemsMap.put(lexeme, framesMap);
        }
    }

}