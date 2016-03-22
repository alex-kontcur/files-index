package ru.bona.fileindex.model.lexem;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.channelworker.ChannelWorkerProvider;
import ru.bona.fileindex.model.Registry;
import ru.bona.fileindex.model.fileparser.ChannelHandler;
import ru.bona.fileindex.model.range.IntRange;

import java.nio.channels.SeekableByteChannel;
import java.util.Map;

/**
 * LexemeRegistry
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */
public class LexemeRegistry extends Registry<LexemControlFile> {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(LexemeRegistry.class);
    private static final String LEXEM_CTRL_FILE = "lexem.ctr";

    /*===========================================[ CLASS METHODS ]================*/

    @Inject
    protected void init(Injector injector, ChannelWorkerProvider workerProvider, Configuration configuration) {
        controlFile = injector.getInstance(LexemControlFile.class);

        String indexPath = configuration.getIndexPath();
        channelWorker = workerProvider.getChannelWorker(indexPath + LEXEM_CTRL_FILE);

        channelWorker.processInputChannel(new ChannelHandler() {
            @Override
            public void handle(SeekableByteChannel channel) throws Exception {
                if (channel.size() > 0) {
                    controlFile.readExternal(channel);
                }
            }
        });
    }

    @Override
    public LexemControlFile getControlFile() {
        return controlFile;
    }

    public void register(String lexeme, Map<Number, IntRange> frameMap) {
        controlFile.put(lexeme, frameMap);
    }

    public Map<String, Map<Number, IntRange>> getFuzzyLexemsMap(String lexeme) {
        return controlFile.getFuzzyLexemsMap(lexeme);
    }

    public Map<Number, IntRange> getFrameInfosByLexeme(String lexeme) {
        return controlFile.getRangeInfosByLexeme(lexeme);
    }

    public void removeFrameInfosForLexeme(String lexeme, Number fileNum) {
        controlFile.removeFrameInfosForLexeme(lexeme, fileNum);
    }

    public Map<Number, IntRange> removeFrameInfosForFile(Number fileNum) {
        return controlFile.removeFrameInfosForFile(fileNum);
    }

    public IntRange getPreviousFrame(String lexeme, Number fileNum) {
        Map<Number, IntRange> regionInfo = getFrameInfosByLexeme(lexeme);
        if (regionInfo != null) {
            IntRange frameInfo = regionInfo.get(fileNum);
            if (frameInfo != null) {
                return frameInfo;
            }
        }
        return null;
    }
}
