package ru.bona.fileindex.model;

import java.io.IOException;
import java.nio.channels.Channel;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Storable
 *
 * @author Kontsur Alex (bona)
 * @since 22.09.14
 */
public interface Storable<C extends Channel> {

    /*===========================================[ INTERFACE METHODS ]============*/

    boolean isStored();

    void store(C channel, ReadWriteLock indexLock) throws IOException;

}
