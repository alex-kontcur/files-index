package ru.bona.fileindex;

import com.google.inject.Inject;
import org.junit.runner.RunWith;
import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.channelworker.ChannelWorkerProvider;
import ru.bona.fileindex.filesholder.FilesHolder;
import ru.bona.fileindex.guice.GuiceTestRunner;
import ru.bona.fileindex.index.IndexService;
import ru.bona.fileindex.model.ControlFile;
import ru.bona.fileindex.model.SourceParser;
import ru.bona.fileindex.model.fileparser.ChannelHandler;
import ru.bona.fileindex.model.lexem.LexemeAnalyzer;
import ru.bona.fileindex.model.lexem.LexemeFrameManager;
import ru.bona.fileindex.model.lexem.LexemeRegistry;
import ru.bona.fileindex.model.lexem.factory.LexemEntryFactory;
import ru.bona.fileindex.model.spec.SpecAnalyzer;
import ru.bona.fileindex.model.spec.SpecFrameManager;
import ru.bona.fileindex.model.spec.SpecRegistry;
import ru.bona.fileindex.search.SearchService;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * AbstractIndexTest
 *
 * @author Kontsur Alex (bona)
 * @since 24.09.14
 */
@RunWith(GuiceTestRunner.class)
public abstract class AbstractIndexTest {

    /*===========================================[ STATIC VARIABLES ]=============*/

    protected static final int TEST_TOKEN_SIZE = 3;

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    @Inject
    protected ChannelWorkerProvider workerProvider;

    @Inject
    protected IndexService indexService;

    @Inject
    protected SearchService searchService;

    @Inject
    protected LexemeFrameManager lexemeFrameManager;

    @Inject
    protected LexemEntryFactory entryFactory;

    @Inject
    protected SpecFrameManager specFrameManager;

    @Inject
    protected LexemeRegistry lexemeRegistry;

    @Inject
    protected LexemeAnalyzer lexemeAnalyzer;

    @Inject
    protected SpecAnalyzer specAnalyzer;

    @Inject
    protected SpecRegistry specRegistry;

    @Inject
    protected SourceParser sourceParser;

    @Inject
    protected FilesHolder filesHolder;

    /*===========================================[ CONSTRUCTORS ]=================*/

    protected void initChannelWorker(String forFileName, final String source, final String encoding) {
        ChannelWorker channelWorker = workerProvider.getChannelWorker(forFileName);
        channelWorker.processOutputChannel(new ChannelHandler() {
            @Override
            public void handle(SeekableByteChannel channel) throws Exception {
                byte[] bytes = source.getBytes(encoding);
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                channel.write(byteBuffer);
            }
        }, true);
    }

    protected void loadControlFile(final ControlFile controlFile, String fileName) {
        String indexPath = Configuration.DEFAULT.getIndexPath();
        ChannelWorker channelWorker = workerProvider.getChannelWorker(indexPath + fileName);
        channelWorker.processInputChannel(new ChannelHandler() {
            @Override
            public void handle(SeekableByteChannel channel) throws Exception {
                if (channel.size() > 0) {
                    controlFile.readExternal(channel);
                }
            }
        });
    }

}
