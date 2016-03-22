package ru.bona.fileindex.model.spec.synctree;

import java.nio.ByteBuffer;

/**
 * IntTree
 *
 * @author Kontsur Alex (bona)
 * @since 21.09.14
 */
public class IntTree extends SyncTree<Integer> {

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public ByteBuffer dumpTree() {
        ByteBuffer buffer = ByteBuffer.allocate(treeSet.size() * 4);
        for (Integer intValue : treeSet) {
            buffer.putInt(intValue);
        }
        return buffer;
    }

    @Override
    public void readTree(ByteBuffer buffer) {
        treeSet.clear();
        while (buffer.hasRemaining()) {
            treeSet.add(buffer.getInt());
        }
    }
}
