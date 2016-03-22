package ru.bona.fileindex;

import org.junit.*;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.listeners.IdxListener;
import ru.bona.fileindex.listeners.SrchListener;
import ru.bona.fileindex.model.SourceParser;
import ru.bona.fileindex.model.lexem.LexemControlFile;
import ru.bona.fileindex.model.lexem.LexemEntry;
import ru.bona.fileindex.model.range.Range;
import ru.bona.fileindex.search.FileSearchInfo;
import ru.bona.fileindex.utils.Helper;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IndexThreadedTest
 *
 * @author Kontsur Alex (bona)
 * @since 26.09.14
 */
public class IndexThreadedTest extends AbstractIndexTest {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private AtomicInteger filesCounter;

    /*===========================================[ CLASS METHODS ]================*/

    @Before
    public void init() {
        workerProvider.clearWorkers();
    }

    @Test
    public void absentCharSequenceFotFoundInTheIndex() throws Exception {
        String test1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<module/>";

        File file = new File(Configuration.DEFAULT.getIndexPath() + "test-0");
        String encoding = "cp1251";
        initChannelWorker(file.getAbsolutePath(), test1, encoding);
        byte[] bytes = test1.getBytes(encoding);
        FileInfo fileInfo = new FileInfo(file, encoding, bytes.length);

        CountDownLatch latch2 = new CountDownLatch(1);
        IdxListener listener = new IdxListener(latch2);
        fileInfo.setNum(0);
        indexService.addFile(fileInfo, listener);
        latch2.await();

        Assert.assertFalse("Assertion error", listener.getFiles().isEmpty());
        Assert.assertNull(listener.getThrowable());

        CountDownLatch latch = new CountDownLatch(1);
        SrchListener srchListener = new SrchListener(latch);
        searchService.searchFor("bla", srchListener);
        latch.await();
        Assert.assertNull(srchListener.getThrowable());
        Collection<FileSearchInfo> matchedFileInfos = srchListener.getMatchedFileInfos();
        Assert.assertTrue(matchedFileInfos.isEmpty());
    }

    @Test
    public void indexWorksCorrectly3() throws Exception {
        String test1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<module/>";
        String encoding = "cp1251";
        File file = new File(Configuration.DEFAULT.getIndexPath() + "test-1");
        initChannelWorker(file.getAbsolutePath(), test1, encoding);
        byte[] bytes = test1.getBytes(encoding);
        FileInfo fileInfo = new FileInfo(file, encoding, bytes.length);
        fileInfo.setNum(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        IdxListener listener = new IdxListener(latch2);
        indexService.addFile(fileInfo, listener);
        latch2.await();
        Assert.assertFalse("Assertion error", listener.getFiles().isEmpty());
        Assert.assertNull(listener.getThrowable());

        CountDownLatch latch = new CountDownLatch(1);
        SrchListener srchListener = new SrchListener(latch);
        searchService.searchFor("?xml", srchListener);
        latch.await();

        Assert.assertNull(srchListener.getThrowable());
        Collection<FileSearchInfo> matchedFileInfos = srchListener.getMatchedFileInfos();
        FileSearchInfo searchInfo = matchedFileInfos.iterator().next();
        Map<File, SortedSet<Range>> fileMap = searchInfo.getFileMap();
        SortedSet<Range> ranges = fileMap.get(file);
        int start = ranges.first().getStart().intValue();
        int stop = ranges.last().getStop().intValue();
        Assert.assertEquals("Assertion error", 2, start);
        Assert.assertEquals("Assertion error", 4, stop);
    }

    @Test
    public void cancelIndexingTest() throws Exception {

        SourceParser testParser = new TestFileParser();
        SourceParser tempParser = sourceParser;
        sourceParser = testParser;

        String test1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<module/> MatchState.MATCHED)) {                FileInf o file Info = filesHolder.getFileInfo(entry.getFileNum());    MATCHED         SortedSet<Range> multiRanges = ntry.getLexemesStartStopRanges(); Map<File, SortedSet<Range>> fileMap = new HashMap<>(); fileMap.put(fileInfo.getFile(), multiRanges);                matchedFileInfos.add(new FileSearchInfo(source, fileMap));} else if (entry.getState().equals(MatchState.MATCH_CANDIDATE";
        File file = new File("test-8");
        String encoding = "cp1251";
        initChannelWorker(file.getAbsolutePath(), test1, encoding);

        CountDownLatch latch = new CountDownLatch(1);
        IdxListener listener = new IdxListener(latch);

        byte[] bytes = test1.getBytes(encoding);
        FileInfo fileInfo = new FileInfo(file, encoding, bytes.length);
        fileInfo.setNum(8);
        indexService.addFile(fileInfo, listener);

        Helper.sleep(600, TimeUnit.MILLISECONDS);

        indexService.stopIndexing(file, listener);
        latch.await();
        fileInfo = filesHolder.getFileInfo("test-8");
        Assert.assertNull(fileInfo);

        sourceParser = tempParser;
    }

    @Test
    public void indexWorksCorrectly() throws Exception {
        String test1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<module/>";
        String test2 = "фыв ак567";

        File file1 = new File("test-3");
        File file2 = new File("test-4");

        String encoding = "cp1251";
        initChannelWorker(file1.getAbsolutePath(), test1, encoding);
        String encoding2 = "utf-8";
        initChannelWorker(file2.getAbsolutePath(), test2, encoding2);

        CountDownLatch latch1 = new CountDownLatch(2);
        IdxListener listener = new IdxListener(latch1);
        byte[] bytes = test1.getBytes(encoding);
        FileInfo fileInfo = new FileInfo(file1, encoding, bytes.length);
        fileInfo.setNum(3);
        indexService.addFile(fileInfo, listener);

        byte[] bytes2 = test2.getBytes(encoding);
        FileInfo fileInfo2 = new FileInfo(file2, encoding2, bytes2.length);
        fileInfo2.setNum(4);
        indexService.addFile(fileInfo2, listener);

        latch1.await();

        Assert.assertFalse("Assertion error", listener.getFiles().isEmpty());
        Assert.assertNull(listener.getThrowable());

        CountDownLatch latch2 = new CountDownLatch(1);
        SrchListener srchListener = new SrchListener(latch2);
        searchService.searchFor("vers", srchListener);
        latch2.await();

        Assert.assertNull(srchListener.getThrowable());
        Collection<FileSearchInfo> matchedFileInfos = srchListener.getMatchedFileInfos();
        FileSearchInfo searchInfo = matchedFileInfos.iterator().next();
        Map<File, SortedSet<Range>> fileMap = searchInfo.getFileMap();
        SortedSet<Range> sortedSet = fileMap.values().iterator().next();

        int start = sortedSet.first().getStart().intValue();
        int stop = sortedSet.last().getStop().intValue();
        Assert.assertEquals(6, start);
        Assert.assertEquals(9, stop);
    }

    @Test
    public void indexWorksCorrectly4() throws Exception {
        String test2 = " {         FileInfo file Info";
        File file2 = new File("test-5");

        String encoding = "cp1251";
        initChannelWorker(file2.getAbsolutePath(), test2, encoding);

        CountDownLatch latch1 = new CountDownLatch(1);
        IdxListener listener = new IdxListener(latch1);
        byte[] bytes2 = test2.getBytes(encoding);
        FileInfo fileInfo2 = new FileInfo(file2, encoding, bytes2.length);
        fileInfo2.setNum(5);
        indexService.addFile(fileInfo2, listener);
        latch1.await();
        Assert.assertFalse("Assertion error", listener.getFiles().isEmpty());
        Assert.assertNull(listener.getThrowable());

        CountDownLatch latch = new CountDownLatch(1);
        SrchListener srchListener = new SrchListener(latch);
        searchService.searchFor("Info", srchListener);
        latch.await();

        Assert.assertNull(srchListener.getThrowable());
        Collection<FileSearchInfo> matchedFileInfos = srchListener.getMatchedFileInfos();
        Assert.assertFalse(matchedFileInfos.isEmpty());
        FileSearchInfo searchInfo = matchedFileInfos.iterator().next();
        Map<File, SortedSet<Range>> fileMap = searchInfo.getFileMap();
        SortedSet<Range> sortedSet = fileMap.values().iterator().next();
        int start = sortedSet.first().getStart().intValue();
        int stop = sortedSet.first().getStop().intValue();
        Assert.assertEquals(15, start);
        Assert.assertEquals(18, stop);
    }

    @Test
    public void indexWorksCorrectly2() throws Exception {
        String test1 = "  MATCHED,NOT_MATCHED,MATCH_CANDIDATE  ";
        String test2 = "(MatchState.MATCHED)) {                FileInf o file Info = filesHolder.getFileInfo(entry.getFileNum());    MATCHED         SortedSet<Range> multiRanges = ntry.getLexemesStartStopRanges(); Map<File, SortedSet<Range>> fileMap = new HashMap<>(); fileMap.put(fileInfo.getFile(), multiRanges);                matchedFileInfos.add(new FileSearchInfo(source, fileMap));} else if (entry.getState().equals(MatchState.MATCH_CANDIDATE";

        File file1 = new File("test-6");
        File file2 = new File("test-7");

        String encoding = "cp1251";
        initChannelWorker(file1.getAbsolutePath(), test1, encoding);
        initChannelWorker(file2.getAbsolutePath(), test2, encoding);

        CountDownLatch latch1 = new CountDownLatch(2);
        IdxListener listener = new IdxListener(latch1);
        byte[] bytes = test1.getBytes(encoding);
        FileInfo fileInfo = new FileInfo(file1, encoding, bytes.length);
        fileInfo.setNum(6);
        indexService.addFile(fileInfo, listener);

        byte[] bytes2 = test2.getBytes(encoding);
        FileInfo fileInfo2 = new FileInfo(file2, encoding, bytes2.length);
        fileInfo2.setNum(7);
        indexService.addFile(fileInfo2, listener);

        latch1.await();
        Assert.assertFalse("Assertion error", listener.getFiles().isEmpty());
        Assert.assertNull(listener.getThrowable());

        LexemEntry lexemEntry = lexemeFrameManager.loadFromIndex("MAT".toCharArray());
        Assert.assertNotNull(lexemEntry);
        Assert.assertEquals("MAT", lexemEntry.getLexeme());
        Map<Number, SortedSet<Range>> fileMap = lexemEntry.getLexemeFileMap();
        for (Map.Entry<Number, SortedSet<Range>> entry : fileMap.entrySet()) {
            Number fileNum = entry.getKey();
            fileInfo = filesHolder.getFileInfo(fileNum);
            String fileName = fileInfo.getFile().getName();
            if (fileName.equals("test-1")) {
                SortedSet<Range> ranges = entry.getValue();
                Assert.assertEquals(3, ranges.size());
            } else if (fileName.equals("test-2")) {
                SortedSet<Range> ranges = entry.getValue();
                Assert.assertEquals(3, ranges.size());
            }
        }

        LexemControlFile controlFile = lexemeRegistry.getControlFile();
        loadControlFile(controlFile, "lexem.ctr");

        LexemEntry lexemEntryTest = lexemeFrameManager.loadFromIndex("fil".toCharArray());

        fileMap = lexemEntryTest.getLexemeFileMap();
        for (Map.Entry<Number, SortedSet<Range>> entry : fileMap.entrySet()) {
            Number fileNum = entry.getKey();
            fileInfo = filesHolder.getFileInfo(fileNum);
            String fileName = fileInfo.getFile().getName();
            if (fileName.equals("test-2")) {
                SortedSet<Range> ranges = entry.getValue();
                Assert.assertEquals(6, ranges.size());
            }
        }

        CountDownLatch latch = new CountDownLatch(1);
        SrchListener srchListener = new SrchListener(latch);
        searchService.searchFor("Info", srchListener);

        latch.await();

        Assert.assertNull(srchListener.getThrowable());
        Collection<FileSearchInfo> matchedFileInfos = srchListener.getMatchedFileInfos();
        Assert.assertFalse(matchedFileInfos.isEmpty());
        FileSearchInfo searchInfo = matchedFileInfos.iterator().next();
        Map<File, SortedSet<Range>> fileMap1 = searchInfo.getFileMap();
        SortedSet<Range> sortedSet = fileMap1.values().iterator().next();
        int start = sortedSet.first().getStart().intValue();
        int stop = sortedSet.first().getStop().intValue();
        Assert.assertEquals(54, start);
        Assert.assertEquals(57, stop);
    }

}
