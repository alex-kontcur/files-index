package ru.bona.fileindex.model;

import ru.bona.fileindex.model.range.IntRange;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Loadable
 *
 * @author Kontsur Alex (bona)
 * @since 23.09.14
 */
public interface Loadable<C extends Channel> {

    /*===========================================[ INTERFACE METHODS ]============*/

    void load(C channel, ReadWriteLock indexLock, Map<Number, IntRange> frameInfoMap) throws IOException;

}
