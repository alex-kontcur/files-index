package ru.bona.fileindex.spec;

import org.junit.Assert;
import org.junit.Test;
import ru.bona.fileindex.AbstractIndexTest;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.model.spec.SpecEntry;
import ru.bona.fileindex.model.spec.synctree.SyncTree;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SpecIndexTest
 *
 * @author Kontsur Alex (bona)
 * @since 25.09.14
 */
public class SpecIndexTest extends AbstractIndexTest {

    /*===========================================[ CLASS METHODS ]================*/

    @Test
    public void specIndexWorksCorrectly() throws Exception {
        String fileName = "test";
        String encoding = "cp1251";
        String test = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<module/>";
        File file = new File(fileName);
        initChannelWorker(file.getAbsolutePath(), test, encoding);

        byte[] bytes = test.getBytes(encoding);
        FileInfo fileInfo = new FileInfo(file, encoding, bytes.length);

        filesHolder.addFileInfo(fileInfo, new ReentrantLock());

        AtomicBoolean terminate = new AtomicBoolean(false);
        specAnalyzer.analyze(fileInfo, terminate);

        SpecEntry specEntry = specFrameManager.loadFromIndex('"');
        Collection<SyncTree> trees = specEntry.getCharacterTrees();
        Assert.assertFalse("Error -> specs were not created correctly ->", trees.isEmpty());
        for (SyncTree tree : trees) {
            Assert.assertFalse("Error -> specs were not created correctly ->", tree.isEmpty());
            Assert.assertEquals("Error -> specs were not created correctly ->", 1, tree.size());
            Assert.assertEquals("Error -> specs were not created correctly ->", 35, tree.getFirst().longValue());
        }
    }

}
