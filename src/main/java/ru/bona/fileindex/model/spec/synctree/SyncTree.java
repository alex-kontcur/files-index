package ru.bona.fileindex.model.spec.synctree;

import java.nio.ByteBuffer;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SyncTree
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */
public abstract class SyncTree<N extends Number> {

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    protected SortedSet<N> treeSet;
    protected ReentrantReadWriteLock lock;

    /*===========================================[ CONSTRUCTORS ]=================*/

    protected SyncTree() {
        treeSet = new TreeSet();
        lock = new ReentrantReadWriteLock();
    }

    /*===========================================[ CLASS METHODS ]================*/

    public void addPosition(N position) {
        lock.writeLock().lock();
        try {
            treeSet.add(position);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean contains(N value) {
        lock.readLock().lock();
        try {
            return treeSet.contains(value);
        } finally {
            lock.readLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public N getFirst(N... from) {
        lock.readLock().lock();
        try {
            return from.length == 0 ? treeSet.first() : treeSet.tailSet(from[0]).first();
        } finally {
            lock.readLock().unlock();
        }
    }

    public N getLast() {
        lock.readLock().lock();
        try {
            return treeSet.last();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        return treeSet.isEmpty();
    }

    public int size() {
        return treeSet.size();
    }

    public abstract ByteBuffer dumpTree();
    public abstract void readTree(ByteBuffer buffer);

}
