package ru.bona.fileindex.channelworker.file;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.model.fileparser.ChannelHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * FileWorker
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public class FileWorker implements ChannelWorker {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(FileWorker.class);

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private String fileName;

    /*===========================================[ CONSTRUCTORS ]=================*/

    @Inject
    public FileWorker(@Assisted String fileName) {
        this.fileName = fileName;
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public void processOutputChannel(ChannelHandler channelHandler, boolean append) {
        try {
            try (FileOutputStream stream = new FileOutputStream(fileName, append)) {
                try (SeekableByteChannel channel = stream.getChannel()) {
                    channelHandler.handle(channel);
                }
            }
        } catch (Exception e) {
            logger.error("Error while process output channel for \"" + fileName + "\" -> ", e);
        }
    }

    @Override
    public void processInputChannel(ChannelHandler channelHandler) {
        try {
            try (FileInputStream stream = new FileInputStream(fileName)) {
                try (SeekableByteChannel channel = stream.getChannel()) {
                    channelHandler.handle(channel);
                }
            }
        } catch (Exception e) {
            logger.error("Error while process input channel for \"{}\" -> {}", fileName, e.getMessage());
        }
    }

    @SuppressWarnings("OverlyNestedMethod")
    @Override
    public void cut(long start, long stop, ReadWriteLock indexLock) {
        File file = new File(fileName);
        String path = file.getAbsolutePath();
        File destFile = new File(path + "-" + UUID.randomUUID().toString());

        indexLock.writeLock().lock();
        try {
            try (FileInputStream is = new FileInputStream(file)) {
                try (FileChannel src = is.getChannel()) {
                    try (FileOutputStream os = new FileOutputStream(destFile)) {
                        try (SeekableByteChannel dst = os.getChannel()) {
                            try {
                                dst.position(dst.size());
                                src.transferTo(0, start, dst);
                                dst.position(dst.size());
                                src.transferTo(stop, src.size(), dst);
                            } catch (Exception e) {
                                logger.error("weeeee", e);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error while deleting region [" + start + ":" + stop + "] from index [" + fileName + "] ->", e);
        } finally {
            indexLock.writeLock().unlock();
        }

        if (file.delete()) {
            destFile.renameTo(new File(path));
        }
    }

}
