package ru.bona.fileindex.model.spec.synctree;

import java.nio.ByteBuffer;

/**
 * ShortTree
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */
public class ShortTree extends SyncTree<Short> {

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public ByteBuffer dumpTree() {
        ByteBuffer buffer = ByteBuffer.allocate(treeSet.size() * 2);
        for (Short shortValue : treeSet) {
            buffer.putShort(shortValue);
        }
        return buffer;
    }

    @Override
    public void readTree(ByteBuffer buffer) {
        treeSet.clear();
        while (buffer.hasRemaining()) {
            treeSet.add(buffer.getShort());
        }
    }
}
