package ru.bona.fileindex.model.spec.synctree;

import java.nio.ByteBuffer;

/**
 * LongTree
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */
public class LongTree extends SyncTree<Long> {

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public ByteBuffer dumpTree() {
        ByteBuffer buffer = ByteBuffer.allocate(treeSet.size() * 8);
        for (Long longValue : treeSet) {
            buffer.putLong(longValue);
        }
        return buffer;
    }

    @Override
    public void readTree(ByteBuffer buffer) {
        treeSet.clear();
        while (buffer.hasRemaining()) {
            treeSet.add(buffer.getLong());
        }
    }
}
