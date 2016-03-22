package ru.bona.fileindex.lexeme;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import ru.bona.fileindex.AbstractIndexTest;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.model.lexem.LexemEntry;
import ru.bona.fileindex.model.range.Range;

import java.io.File;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LexemeIndexTest
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */
public class LexemeIndexTest extends AbstractIndexTest {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private FileInfo fileInfo;

    /*===========================================[ CLASS METHODS ]================*/

    @Before
    public void initMocks() throws Exception {
        MockitoAnnotations.initMocks(this);

        String fileName = "test";
        String encoding = "cp1251";
        String test = "import ru.bona.web.crawler.auto.model.autoru.AutoruUserData;";
        byte[] bytes = test.getBytes(encoding);
        File file = new File(fileName);
        initChannelWorker(file.getAbsolutePath(), test, encoding);

        fileInfo = new FileInfo(file, encoding, bytes.length);
        filesHolder.addFileInfo(fileInfo, new ReentrantLock());
    }

    @Test
    public void lexemIndexWorksCorrectly() {
        AtomicBoolean terminate = new AtomicBoolean(false);
        lexemeAnalyzer.analyze(fileInfo, terminate);

        LexemEntry lexemEntry = lexemeFrameManager.loadFromIndex("uUs".toCharArray());
        Assert.assertNotNull(lexemEntry);

        Assert.assertFalse("Error -> lexems were not created correctly ->", lexemEntry.getLexeme().isEmpty());
        Assert.assertFalse("Error -> lexems were not created correctly ->", lexemEntry.getLexemeFileMap().isEmpty());
        Map<Number, SortedSet<Range>> lexemeFileMap = lexemEntry.getLexemeFileMap();
        for (Map.Entry<Number, SortedSet<Range>> entry : lexemeFileMap.entrySet()) {
            Number fileNum = entry.getKey();
            SortedSet<Range> ranges = entry.getValue();

            Assert.assertEquals("Error -> lexems were not created correctly ->", fileNum, fileInfo.getNum());
            Assert.assertFalse("Error -> lexems were not created correctly ->", ranges.isEmpty());
            Range first = ranges.first();
            Assert.assertEquals("Error -> lexems were not created correctly ->", 50, first.getStart().intValue());
            Assert.assertEquals("Error -> lexems were not created correctly ->", 52, first.getStop().intValue());
        }
    }

}
