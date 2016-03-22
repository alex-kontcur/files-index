package ru.bona.fileindex.channelworker.bytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * BytesChannel
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public class BytesChannel implements SeekableByteChannel {

    /*===========================================[ STATIC VARIABLES ]=============*/

    private static final ThreadLocal<AtomicInteger> position = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
        }
    };

    private static final ThreadLocal<AtomicInteger> index = new ThreadLocal<AtomicInteger>() {
        @Override
        protected AtomicInteger initialValue() {
            return new AtomicInteger(0);
        }
    };

    /*===========================================[ INSTANCE VARIABLES ]===========*/

    private ReentrantReadWriteLock lock;
    private List<byte[]> streamList;
    private AtomicInteger size;

    /*===========================================[ CONSTRUCTORS ]=================*/

    public BytesChannel() {
        lock = new ReentrantReadWriteLock();
        streamList = new ArrayList<>();
        size = new AtomicInteger(0);
        reset();
    }

    /*===========================================[ CLASS METHODS ]================*/

    public SeekableByteChannel cut(long start, long stop) throws IOException {
        lock.writeLock().lock();
        try {
            position(0);
            ByteBuffer buffer1 = ByteBuffer.allocate((int) start);
            read(buffer1);
            ByteBuffer buffer2 = ByteBuffer.allocate((int) (stop - start + 1));
            read(buffer2);
            ByteBuffer buffer3 = ByteBuffer.allocate((int) (size.get() - stop - 1));
            read(buffer3);

            truncate(0);
            ByteBuffer buffer = ByteBuffer.allocate(buffer1.capacity() + buffer3.capacity());

            buffer1.flip();
            buffer.put(buffer1);

            buffer3.flip();
            buffer.put(buffer3);
            write(buffer);

            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static void reset() {
        index.get().set(0);
        position.get().set(0);
    }

    @Override
    public long position() throws IOException {
        lock.readLock().lock();
        try {
            int pos = 0;
            for (int i = 0; i < index.get().get(); i++) {
                pos += streamList.get(i).length;
            }
            pos += position.get().get();
            return pos;
        } finally {
            lock.readLock().unlock();
        }            
    }

    @Override
    public long size() throws IOException {
        return size.get();
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        reset();
        int capacity = (int) newPosition;
        ByteBuffer buffer = ByteBuffer.allocate(capacity);
        read(buffer);
        return this;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        lock.writeLock().lock();
        try {
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            reset();
            read(buffer);
            streamList.clear();
            write(buffer);
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int capacity = dst.capacity();
        if (capacity == 0) {
            return 0;
        }

        int totalCnt = 0;
        lock.readLock().lock();
        try {
            while (index.get().get() < streamList.size() && totalCnt < capacity) {
                byte[] bytes = streamList.get(index.get().get());
                int length = bytes.length;
                int size = length < capacity - totalCnt ? length : capacity - totalCnt;
                if (size > length - position.get().get()) {
                    size = length - position.get().get();
                }
                byte[] temp = new byte[size];
                System.arraycopy(bytes, position.get().get(), temp, 0, temp.length);
                dst.put(temp);

                position.get().addAndGet(size);
                if (position.get().get() >= length) {
                    position.get().set(0);
                    index.get().incrementAndGet();
                }
                totalCnt += temp.length;
            }
        } finally {
            lock.readLock().unlock();
        }
        return totalCnt;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        byte[] bytes = src.array();
        if (bytes.length == 0) {
            return 0;
        }
        lock.writeLock().lock();
        try {
            streamList.add(bytes);
            size.addAndGet(bytes.length);
        } finally {
            lock.writeLock().unlock();
        }
        return bytes.length;
    }

}
