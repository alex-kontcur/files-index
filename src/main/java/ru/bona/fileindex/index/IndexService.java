package ru.bona.fileindex.index;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.bona.fileindex.Configuration;
import ru.bona.fileindex.filesholder.FileInfo;
import ru.bona.fileindex.filesholder.FilesHolder;
import ru.bona.fileindex.index.builder.IndexBuilder;
import ru.bona.fileindex.index.builder.IndexBuilderFactory;
import ru.bona.fileindex.model.lexem.LexemeFrameManager;
import ru.bona.fileindex.model.spec.SpecFrameManager;
import ru.bona.fileindex.utils.Helper;

import java.io.File;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * IndexService
 *
 * @author Kontsur Alex (bona)
 * @since 18.09.14
 */
public class IndexService {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final Logger logger = LoggerFactory.getLogger(IndexService.class);
    private static final int STOP_INDEXING_WAIT_PERIOD = 1;

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private ReentrantLock lock;
    private AtomicBoolean backupStarted;

    private Semaphore semaphore;
    private Map<String, Future> processingFiles;
    private Map<String, IndexBuilder> workingBuilders;
    private Queue<IndexTask> awaitingTasks;
    private ExecutorService filesExecutor;

    private FilesHolder filesHolder;
    private SpecFrameManager specFrameManager;
    private LexemeFrameManager lexemeFrameManager;
    private IndexBuilderFactory indexBuilderFactory;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public IndexService() {
        lock = new ReentrantLock();
        backupStarted = new AtomicBoolean(false);
        awaitingTasks = new ConcurrentLinkedQueue<>();
        processingFiles = new ConcurrentHashMap<>();
        workingBuilders = new ConcurrentHashMap<>();
    }

    /*===========================================[ CLASS METHODS ]================*/

    @Inject
    protected void init(FilesHolder filesHolder, SpecFrameManager specFrameManager, LexemeFrameManager lexemeFrameManager,
                     IndexBuilderFactory indexBuilderFactory, Configuration configuration) {

        this.filesHolder = filesHolder;
        this.specFrameManager = specFrameManager;
        this.lexemeFrameManager = lexemeFrameManager;
        this.indexBuilderFactory = indexBuilderFactory;


        int simThreads = configuration.getSimIndexThreads();
        semaphore = new Semaphore(simThreads);
        filesExecutor = Helper.prepareFilesThreadPool("INDX-Worker", simThreads);

        ScheduledExecutorService pollExecutor = Executors.newSingleThreadScheduledExecutor();
        pollExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                runIndexing();
            }
        }, 500, 500, TimeUnit.MILLISECONDS);

        ScheduledExecutorService backupExecutor = Executors.newSingleThreadScheduledExecutor();
        backupExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                backup();
            }
        }, 15, 15, TimeUnit.MINUTES);
    }


    public void addFile(FileInfo fileInfo, IndexingListener listener) {
        File file = fileInfo.getFile();
        if (filesHolder.isRegistered(file)) {
            listener.onFailure(file, new Throwable("File " + file.getName() + " already in process"));
        } else {
            filesHolder.addFileInfo(fileInfo, lock);
            awaitingTasks.add(new IndexTask(fileInfo, listener));
        }
    }

    public void removeFile(File file) {
        filesHolder.removeFile(file);
    }

    public boolean isIndexing(File file) {
        return workingBuilders.get(file.getAbsolutePath()) != null;
    }

    public void stopIndexing(File file, IndexingListener listener) {
        FileInfo fileInfo = filesHolder.getFileInfo(file.getAbsolutePath());

        logger.info("Stopping indexing file \"{}\"", fileInfo.getFile().getName());

        awaitingTasks.remove(new IndexTask(fileInfo));
        String fileName = fileInfo.getFullPath();
        Future future = processingFiles.remove(fileName);
        if (future == null) {
            logger.info("File {} is not processing", fileName);
            listener.onFailure(file, new Throwable("File \"" + fileName + "\" is not processing"));
        } else {
            if (!future.isDone()) {
                IndexBuilder indexBuilder = workingBuilders.get(fileName);
                if (indexBuilder != null) {
                    indexBuilder.setInterrupted(true);
                }
                logger.info("Awaiting for graceful shutdown of indexing of file -> {}", fileName);
                Helper.sleep(STOP_INDEXING_WAIT_PERIOD, TimeUnit.SECONDS);
                future.cancel(true);
                logger.info("Indexing stopped for file -> {}", fileName);
                removeFile(file);
                listener.onSuccess(file);
            }
        }
    }

    private void runIndexing() {
        try {
            while (!awaitingTasks.isEmpty()) {
                IndexTask indexTask = awaitingTasks.poll();
                Future future = submitTask(indexTask);
                logger.info("{} submitted", indexTask.getFileInfo());
                if (future != null) {
                    FileInfo fileInfo = indexTask.getFileInfo();
                    processingFiles.put(fileInfo.getFullPath(), future);
                }
            }
        } catch (Throwable t) {
            logger.error("Error while running order processors : ", t);
        }
    }

    private Future submitTask(IndexTask indexTask) {
        Future future = null;
        try {
            semaphore.acquire();

            while (backupStarted.get()) {
                Helper.sleep(100, TimeUnit.MILLISECONDS);
            }

            final FileInfo fileInfo = indexTask.getFileInfo();
            final IndexingListener listener = indexTask.getListener();

            future = filesExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        IndexBuilder indexBuilder = indexBuilderFactory.createIndexBuilder();
                        logger.info("Indexing started for {}", fileInfo.getFile().getName());
                        workingBuilders.put(fileInfo.getFullPath(), indexBuilder);
                        indexBuilder.processFile(fileInfo);
                        logger.info("Indexing stopped for {}", fileInfo.getFile().getName());
                        fileInfo.setProcessed(true);
                        listener.onSuccess(fileInfo.getFile());
                    } catch (Throwable t) {
                        logger.info("Indexing failed for {} -> {}", fileInfo.getFile().getName(), t.getMessage());
                        listener.onFailure(fileInfo.getFile(), t);
                    }
                    workingBuilders.remove(fileInfo.getFullPath());
                    processingFiles.remove(fileInfo.getFullPath());
                    semaphore.release();
                }
            });

        } catch (InterruptedException e) {
            logger.error("Waiting for submitTask is interrupted ->", e);
        }
        return future;
    }

    public void backup() {
        backupStarted.set(true);

        lock.lock();
        try {
            //waiting for current processing files are compeleted
            while (!processingFiles.isEmpty()) {
                Helper.sleep(100, TimeUnit.MILLISECONDS);
            }
            Helper.sleep(STOP_INDEXING_WAIT_PERIOD, TimeUnit.SECONDS);

            //1. После того, как мы заходим в секцию lexemeIndexLock.writeLock().lock()
            //   мы уже имеем актуальный индекс lexem.idx.bak

            //2. Мы должны сделать дамп реестра, дамп fileHolder'а, копию lexem.idx(.bak)
            //   Операция backup делается периодично + при правильном завершении работы библиотеки
            //   вызовом метода IndexService.stop();
            //   Если завершение работы было правильным, то файлы lexem.idx и lexem.idx.bak будут одинаковыми,
            //   и поднятый lexemControlFile из lexem.ctr будет соответствовать обоим файлам (то же и для files.info)
            //   Если завершение работы было аварийным, то файлы lexem.idx и lexem.idx.bak будут разными,
            //   и поднятый lexemControlFile из lexem.ctr будет соответствовать только lexem.idx.bak (то же и для files.info)
            //   Поэтому при старте, если файлы lexem.idx и lexem.idx.bak разные, мы копируем lexem.idx.bak в lexem.idx

            boolean specBackuped = specFrameManager.backup();
            boolean lexemeBackuped = lexemeFrameManager.backup();
            if (specBackuped && lexemeBackuped) {
                filesHolder.createDump();
            } else {
                logger.warn("DUMP was not created, see logs for details.");
            }

            //3. После этих шагов мы имеем в папке index (для lexem индекса) :
            //   - lexem.ctr, lexem.ctr.bak, lexem.idx, files.info (в этом наборе уже сразу после начала
            //   индексирования следующего файла только lexem.idx будет соответствовать реальным данным в памяти), а
            //   набор (lexem.ctr.bak, lexem.idx, files.info) всегда консистентный и на него можно откатиться

        } finally {
            lock.unlock();
            backupStarted.set(false);
        }
    }

}
