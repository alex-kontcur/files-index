package ru.bona.fileindex.model.spec.synctree;

import java.nio.ByteBuffer;

/**
 * ByteTree
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */
public class ByteTree extends SyncTree<Byte> {

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public ByteBuffer dumpTree() {
        ByteBuffer buffer = ByteBuffer.allocate(treeSet.size());
        for (Byte byteValue : treeSet) {
            buffer.put(byteValue);
        }
        return buffer;
    }

    @Override
    public void readTree(ByteBuffer buffer) {
        treeSet.clear();
        while (buffer.hasRemaining()) {
            treeSet.add(buffer.get());
        }
    }
}
