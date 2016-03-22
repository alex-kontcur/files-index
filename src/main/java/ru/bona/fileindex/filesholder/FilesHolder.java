package ru.bona.fileindex.filesholder;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.channelworker.ChannelWorker;
import ru.bona.fileindex.channelworker.ChannelWorkerProvider;
import ru.bona.fileindex.index.IndexService;
import ru.bona.fileindex.index.IndexingListener;
import ru.bona.fileindex.model.BufferInfo;
import ru.bona.fileindex.model.fileparser.FileParser;
import ru.bona.fileindex.model.lexem.LexemeFrameManager;
import ru.bona.fileindex.model.spec.SpecFrameManager;
import ru.bona.fileindex.utils.Helper;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * FilesHolder
 *
 * @author Kontsur Alex (bona)
 * @since 16.09.14
 */
public class FilesHolder {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(FilesHolder.class);

    private static final String FILES_REGISTRY_ENCODING = "cp1251";

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private ReentrantLock lock;
    private String filesRegistry;
    private Map<String, FileInfo> files;

    private SpecFrameManager specFrameManager;
    private LexemeFrameManager lexemeFrameManager;

    /*===========================================[ CLASS METHODS ]================*/

    @Inject
    protected void init(IndexService indexService, SpecFrameManager specFrameManager, Configuration configuration,
                        LexemeFrameManager lexemeFrameManager, FileParser fileParser, ChannelWorkerProvider provider) {

        this.specFrameManager = specFrameManager;
        this.lexemeFrameManager  = lexemeFrameManager;

        String indexPath = configuration.getIndexPath();
        File path = new File(indexPath);
        if (!path.exists()) {
            path.mkdir();
        }

        filesRegistry = indexPath + "files.info";
        lock = new ReentrantLock();
        files = new ConcurrentHashMap<>();

        File file = new File(filesRegistry);
        long fileSize = file.length();
        if (fileSize > 0) {
            ChannelWorker channelWorker = provider.getChannelWorker(file.getAbsolutePath());
            BufferInfo info = fileParser.readChars(channelWorker, FILES_REGISTRY_ENCODING, 0, file.length());
            String source = info.getSourcePart();
            loadFiles(source, indexService);
        }

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String, FileInfo> infoEntry : files.entrySet()) {
                    FileInfo fileInfo = infoEntry.getValue();
                    if (fileInfo.isExists()) {
                        continue;
                    }
                    removeFile(fileInfo.getFile());
                }
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    public void removeFile(File file) {
        String filePath = file.getAbsolutePath();
        lock.lock();
        try {
            FileInfo fileInfo = files.remove(filePath);
            if (fileInfo != null) {
                String fileName = fileInfo.getFile().getName();
                logger.info("Deleting file \"{}\" from index started", fileName);
                specFrameManager.deleteFromIndex(fileInfo.getNum());
                lexemeFrameManager.deleteFromIndex(fileInfo.getNum());
                logger.info("Deleting file \"{}\" from index stopped", fileName);
            }
        } finally {
            lock.unlock();
        }
    }

    private void loadFiles(String source, IndexService indexService) {
        if (source.isEmpty()) {
            return;
        }
        List<FileInfo> unprocessed = new ArrayList<>();
        String[] fileInfos = source.split(System.getProperty("line.separator"));
        int filesCount = fileInfos.length;
        for (String fileInfoString : fileInfos) {
            String[] parts = fileInfoString.split(";");
            Number fileNum = Helper.getNumber(filesCount, parts[0]);
            Boolean processed = Boolean.parseBoolean(parts[1]);
            String pathname = parts[2];
            File file = new File(pathname);
            FileInfo fileInfo = new FileInfo(file, parts[3]);
            fileInfo.setNum(fileNum);
            files.put(pathname, fileInfo);
            if (!processed) {
                unprocessed.add(fileInfo);
            }
        }
        for (FileInfo fileInfo : unprocessed) {
            indexService.addFile(fileInfo, createRestartedListener());
        }
    }

    public void createDump() {
        lock.lock();
        try {
            new File(filesRegistry).delete();
            try (PrintWriter writer = new PrintWriter(filesRegistry, FILES_REGISTRY_ENCODING)) {
                for (Map.Entry<String, FileInfo> infoEntry : files.entrySet()) {
                    FileInfo info = infoEntry.getValue();
                    writer.print(info.getNum());
                    writer.print(";");
                    writer.print(info.getProcessed());
                    writer.print(";");
                    writer.print(info.getFullPath());
                    writer.print(";");
                    writer.println(info.getEncoding());
                }
            }
        } catch (Exception e) {
            logger.error("Error while storing file \"" + filesRegistry + "\" ->", e);
        } finally {
            lock.unlock();
        }
    }

    public void addFileInfo(FileInfo fileInfo, Lock lock) {
        lock.lock();
        try {
            String fullPath = fileInfo.getFullPath();
            SortedSet<Number> set = new TreeSet<>();
            for (FileInfo info : files.values()) {
                set.add(info.getNum());
            }
            long i = 0;
            int filesCount = set.size();
            for (Number number : set) {
                if (number.longValue() != i) {
                    Number fileNum = Helper.getNumber(filesCount, i);
                    fileInfo.setNum(fileNum);
                    files.put(fullPath, fileInfo);
                    return;
                }
                i++;
            }
            Number fileNum = Helper.getNumber(filesCount == 0 ? 0 : filesCount + 1, i);
            fileInfo.setNum(fileNum);
            files.put(fullPath, fileInfo);
        } finally {
            lock.unlock();
        }
    }

    public boolean isRegistered(File file) {
        return getFileInfo(file.getAbsolutePath()) != null;
    }

    public FileInfo getFileInfo(String fullPath) {
        return files.get(fullPath);
    }

    public FileInfo getFileInfo(Number num) {
        for (FileInfo fileInfo : files.values()) {
            if (fileInfo.getNum().intValue() == num.intValue()) {
                return fileInfo;
            }
        }
        return null;
    }

    public long getFilesCount() {
        return files.size();
    }

    public static IndexingListener createRestartedListener() {
        return new IndexingListener() {
            @Override
            public void onSuccess(File file) {
                logger.info("File \"{}\" indexing proceed after restarting", file);
            }
            @Override
            public void onFailure(File file, Throwable t) {
                logger.info("File \"{}\" indexing failed after restarting -> {}", file, t.getMessage());
            }
        };
    }
}
