package ru.bona.fileindex.channelworker;

import com.google.inject.Inject;

import java.util.HashMap;
import java.util.Map;

/**
 * AbstractWorkerProvider
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public abstract class AbstractWorkerProvider implements ChannelWorkerProvider {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private Map<String, ChannelWorker> workerMap;

    protected ChannelWorkerFactory factory;

    /*===========================================[ CONSTRUCTORS ]=================*/

    @Inject
    public void init(ChannelWorkerFactory factory) {
        this.factory = factory;

        workerMap = new HashMap<>();
    }

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public ChannelWorker getChannelWorker(String fileName) {
        ChannelWorker fileWorker = workerMap.get(fileName);
        if (fileWorker == null) {
            fileWorker = createWorker(fileName);
            workerMap.put(fileName, fileWorker);
        }
        return fileWorker;
    }

    @Override
    public void clearWorkers() {
        workerMap.clear();
    }

    protected abstract ChannelWorker createWorker(String fileName);
}
