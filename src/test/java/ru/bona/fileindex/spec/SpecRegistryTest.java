package ru.bona.fileindex.spec;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.bona.fileindex.AbstractIndexTest;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.model.range.IntRange;
import ru.bona.fileindex.model.range.Range;
import ru.bona.fileindex.model.spec.SpecControlFile;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SpecRegistryTest
 *
 * @author Kontsur Alex (bona)
 * @since 20.09.14
 */
public class SpecRegistryTest extends AbstractIndexTest {

    /*===========================================[ CLASS METHODS ]================*/

    @Before
    public void setup() throws Exception {
        ConcurrentHashMap<Number, IntRange> ctrMap = new ConcurrentHashMap<>();
        IntRange range = new IntRange();
        range.setStart(0);
        range.setStop(10);
        ctrMap.put(0, range);
        specRegistry.register('?', ctrMap);

        ReentrantLock lock = new ReentrantLock();
        filesHolder.addFileInfo(new FileInfo(new File("."), "", 0), lock);

        specRegistry.createDump();
    }

    @Test
    public void specIndexCreatedCorectly() {
        SpecControlFile controlFile = specRegistry.getControlFile();
        loadControlFile(controlFile, "spec.ctr");

        Map<Number, IntRange> infoMap = specRegistry.getFrameInfosByChar('?');

        Assert.assertNotNull("Error -> specControlFile was not stored -> ", infoMap);
        Range frameInfo = infoMap.get((byte) 0);
        byte start = frameInfo.getStart().byteValue();
        byte stop = frameInfo.getStop().byteValue();
        Assert.assertEquals("Error -> start value was not stored -> ", 0, start);
        Assert.assertEquals("Error -> stop value was not stored -> ", 10, stop);
    }

}
