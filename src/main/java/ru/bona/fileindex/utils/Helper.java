package ru.bona.fileindex.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.model.range.*;
import ru.bona.fileindex.model.spec.synctree.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * Helper
 *
 * @author Kontsur Alex (bona)
 * @since 23.09.14
 */
public class Helper {

    /*===========================================[ STATIC VARIABLES ]=============*/

    public static final String SEP = System.getProperty("line.separator");

    private static final Logger logger = LoggerFactory.getLogger(Helper.class);

    /*===========================================[ CONSTRUCTORS ]=================*/

    private Helper() {
    }

    /*===========================================[ CLASS METHODS ]================*/

    public static boolean copyFile(String sourceFile, String backupFile) {
        Path source = Paths.get(sourceFile);
        Path backup = Paths.get(backupFile);
        boolean res = true;
        try {
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Error while copying " + sourceFile + " to " + backupFile + " -> ", e);
            res = false;
        }
        return res;
    }

    public static <T> void splitThread(Iterable<T> iterable, final SplittedRunnable<T> runnable) {
        int count = 0;
        for (T item : iterable) {
            count++;
        }
        if (count == 0) {
            return;
        }

        final CountDownLatch latch = new CountDownLatch(count);
        ExecutorService searchService = Executors.newFixedThreadPool(count);
        for (final T element : iterable) {
            searchService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run(element);
                    } catch (Throwable t) {
                        logger.error("Error while splitting thread ->", t);
                    }
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (Exception ignored) {
            logger.error("Waiting for processing splitThread is interrupted");
        }
    }

    public static boolean isTwoBytesChar(char c) {
        return !Character.UnicodeBlock.of(c).equals(Character.UnicodeBlock.BASIC_LATIN);
    }

    public static ThreadPoolExecutor prepareFilesThreadPool(final String threadName, int simThreads) {
        ThreadFactory threadFactory = new ThreadFactory() {
            private AtomicInteger threads = new AtomicInteger();
            @Override
            public Thread newThread(Runnable r) {
                String nameBase = threadName + " #%d";
                return new Thread(r, format(nameBase, threads.getAndIncrement()));
            }
        };
        return new ThreadPoolExecutor(
            simThreads * 2,
            simThreads * 2,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(simThreads * 4),
            threadFactory
        );
    }

    public static CharsetDecoder prepareDecoder(String encoding) {
        Charset charset = Charset.forName(encoding);
        CharsetDecoder decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        return decoder;
    }

    public static SyncTree makeSyncTree(long fileSize) {
        if (fileSize < Byte.MAX_VALUE) {
            return new ByteTree();
        } else if (fileSize < Short.MAX_VALUE) {
            return new ShortTree();
        } else if (fileSize < Integer.MAX_VALUE) {
            return new IntTree();
        } else {
            return new LongTree();
        }
    }
    public static Range makeRange(long fileSize) {
        if (fileSize < Byte.MAX_VALUE) {
            return new ByteRange();
        } else if (fileSize < Short.MAX_VALUE) {
            return new ShortRange();
        } else if (fileSize < Integer.MAX_VALUE) {
            return new IntRange();
        } else {
            return new LongRange();
        }
    }


    /**
     * Этот метод используется при сохранении контрол файлов для того, чтобы приводить все номера файлов в системе
     * к единому размеру в зависимости от количества файлов.
     *
     * @param filesCount
     * @param fileNum
     * @return
     */

    public static ByteBuffer getByteBuffer(long filesCount, Number fileNum) {
        if (filesCount < Byte.MAX_VALUE) {
            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put(fileNum.byteValue());
            return buffer;
        } else if (filesCount < Short.MAX_VALUE) {
            ByteBuffer buffer = ByteBuffer.allocate(2);
            buffer.putShort(fileNum.shortValue());
            return buffer;
        } else if (filesCount < Integer.MAX_VALUE) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(fileNum.intValue());
            return buffer;
        } else {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putLong(fileNum.longValue());
            return buffer;
        }
    }

    public static Number getNumber(long filesCount, long value) {
        if (filesCount < Byte.MAX_VALUE) {
            return Long.valueOf(value).byteValue();
        } else if (filesCount < Short.MAX_VALUE) {
            return Long.valueOf(value).shortValue();
        } else if (filesCount < Integer.MAX_VALUE) {
            return Long.valueOf(value).intValue();
        } else {
            return value;
        }
    }

    public static Number getNumber(long filesCount, String value) {
        if (filesCount < Byte.MAX_VALUE) {
            return Byte.parseByte(value);
        } else if (filesCount < Short.MAX_VALUE) {
            return Short.parseShort(value);
        } else if (filesCount < Integer.MAX_VALUE) {
            return Integer.parseInt(value);
        } else {
            return Long.parseLong(value);
        }
    }

    public static <N extends Number> N adaptValueForSize(long start, int i, long fileSize, Number number) {
        if (fileSize < Byte.MAX_VALUE) {
            return (N) (Byte) (byte) (start + i + number.byteValue());
        } else if (fileSize < Short.MAX_VALUE) {
            return (N) (Short) (short) (start + i + number.shortValue());
        } else if (fileSize < Integer.MAX_VALUE) {
            return (N) (Integer) (int) (start + i + number.intValue());
        } else {
            return (N) (Long) (long) (start + i + number.longValue());
        }
    }

    public static void sleep(long interval, TimeUnit timeUnit) {
        try {
            timeUnit.sleep(interval);
        } catch (InterruptedException ignored) {
        }
    }

    public static Number readNumFromChannel(long filesCount, SeekableByteChannel channel) throws IOException {
        if (filesCount < Byte.MAX_VALUE) {
            ByteBuffer buffer = ByteBuffer.allocate(1);
            channel.read(buffer);
            buffer.flip();
            return buffer.get();
        } else if (filesCount < Short.MAX_VALUE) {
            ByteBuffer buffer = ByteBuffer.allocate(2);
            channel.read(buffer);
            buffer.flip();
            return buffer.getShort();
        } else if (filesCount < Integer.MAX_VALUE) {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            channel.read(buffer);
            buffer.flip();
            return buffer.getInt();
        } else {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            channel.read(buffer);
            buffer.flip();
            return buffer.getLong();
        }
    }

}
