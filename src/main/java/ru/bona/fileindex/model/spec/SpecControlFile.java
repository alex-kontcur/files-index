package ru.bona.fileindex.model.spec;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.model.ControlFile;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.utils.Helper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
* SpecControlFile
*
* @author Kontsur Alex (bona)
* @since 21.09.14
*/
public class SpecControlFile extends ControlFile {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(SpecControlFile.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private Map<Character, Map<Number, IntRange>> specMap;

    /*===========================================[ CLASS METHODS ]================*/

    @Inject
    public SpecControlFile() {
        specMap = new ConcurrentHashMap<>();
        for (char c : SpecRegistry.SPEC_SYMBOLS) {
            specMap.put(c, new ConcurrentHashMap<Number, IntRange>());
        }
    }

    public void put(char c, Map<Number, IntRange> frameMap) {
        Map<Number, IntRange> infoMap = getCharacterInfoMap(c);
        try {
            if (infoMap.isEmpty()) {
                infoMap.putAll(frameMap);
            } else {
                for (Map.Entry<Number, IntRange> entry : frameMap.entrySet()) {
                    Number fileNum = entry.getKey();
                    infoMap.put(fileNum, entry.getValue());
                }
            }
            specMap.put(c, frameMap);
        } catch (Exception e) {
            logger.error("Error while put char " + c + " ->", e);
        }
    }

    public Map<Number, IntRange> getCharacterInfoMap(char c) {
        return specMap.get(c);
    }

    public Map<Number, IntRange> removeFrameInfosByFile(Number fileNum) {
        Map<Number, IntRange> filesMap = new HashMap<>();
        for (Map.Entry<Character, Map<Number, IntRange>> entry : specMap.entrySet()) {
            Map<Number, IntRange> infoMap = entry.getValue();
            filesMap.put(fileNum, infoMap.remove(fileNum));
        }
        return filesMap;
    }

    public void removeFrameInfosByCharAndFile(Character c, Number fileNum) {
        Map<Number, IntRange> map = getCharacterInfoMap(c);
        map.remove(fileNum);
    }

    @Override
    public void writeExternal(SeekableByteChannel channel) throws IOException {
        long filesCount = filesHolder.getFilesCount();
        for (Map.Entry<Character, Map<Number, IntRange>> entry : specMap.entrySet()) {
            Character character = entry.getKey();
            Map<Number, IntRange> pairMap = entry.getValue();
            if (pairMap.isEmpty()) {
                continue;
            }
            ByteBuffer valueBuffer = ByteBuffer.allocate(2);
            valueBuffer.putChar(character);
            saveBuffer(channel, valueBuffer);

            int filesCountForChar = pairMap.entrySet().size();
            valueBuffer = Helper.getByteBuffer(filesCount, filesCountForChar);
            saveBuffer(channel, valueBuffer);

            saveFileFrameMap(channel, pairMap);
        }
    }

    @Override
    public void readExternal(SeekableByteChannel channel) throws IOException {
        specMap.clear();

        channel.position(0);
        while (channel.position() < channel.size()) {
            ByteBuffer buffer = ByteBuffer.allocate(2);
            channel.read(buffer);
            buffer.flip();

            CharBuffer cBuffer = buffer.asCharBuffer();
            Character c = cBuffer.get();

            Number filesCount = Helper.readNumFromChannel(filesHolder.getFilesCount(), channel);

            Map<Number, IntRange> framesMap = new HashMap<>();
            fillRangeMap(framesMap, channel, filesCount);

            specMap.put(c, framesMap);
        }
    }

}
