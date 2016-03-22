package ru.bona.fileindex.lexeme;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.bona.fileindex.AbstractIndexTest;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.model.lexem.LexemControlFile;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.model.range.Range;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LexemeRegistryTest
 *
 * @author Kontsur Alex (bona)
 * @since 24.09.14
 */
public class LexemeRegistryTest extends AbstractIndexTest {

    /*===========================================[ CLASS METHODS ]================*/

    @Before
    public void setup() throws Exception {
        Map<Number, IntRange> ctrMap = new ConcurrentHashMap<>();
        IntRange range = new IntRange();
        range.setStart(0);
        range.setStop(10);
        ctrMap.put(0, range);

        range = new IntRange();
        range.setStart(1);
        range.setStop(2);
        ctrMap.put(1, range);

        lexemeRegistry.register("ячс", ctrMap);

        Map<Number, IntRange> ctrMap2 = new ConcurrentHashMap<>();
        range = new IntRange();
        range.setStart(11);
        range.setStop(12);

        ctrMap2.put(0, range);
        lexemeRegistry.register("wer", ctrMap2);

        ReentrantLock lock = new ReentrantLock();
        filesHolder.addFileInfo(new FileInfo(new File("."), "", 0), lock);

        lexemeRegistry.createDump();
    }

    @Test
    public void specIndexCreatedCorectly() {
        LexemControlFile controlFile = lexemeRegistry.getControlFile();
        loadControlFile(controlFile, "lexem.ctr");

        Map<Number, IntRange> infoMap = lexemeRegistry.getFrameInfosByLexeme("ячс");

        Assert.assertNotNull("Error -> specControlFile was not stored -> ", infoMap);
        Range frameInfo = infoMap.get((byte) 0);
        int start = frameInfo.getStart().intValue();
        int stop = frameInfo.getStop().intValue();
        Assert.assertEquals("Error -> start value was not stored -> ", 0, start);
        Assert.assertEquals("Error -> stop value was not stored -> ", 10, stop);

        frameInfo = infoMap.get((byte) 1);
        start = frameInfo.getStart().intValue();
        stop = frameInfo.getStop().intValue();
        Assert.assertEquals("Error -> start value was not stored -> ", 1, start);
        Assert.assertEquals("Error -> stop value was not stored -> ", 2, stop);

        infoMap = lexemeRegistry.getFrameInfosByLexeme("wer");

        Assert.assertNotNull("Error -> specControlFile was not stored -> ", infoMap);
        frameInfo = infoMap.get((byte) 0);
        start = frameInfo.getStart().intValue();
        stop = frameInfo.getStop().intValue();
        Assert.assertEquals("Error -> start value was not stored -> ", 11, start);
        Assert.assertEquals("Error -> stop value was not stored -> ", 12, stop);
    }

}
