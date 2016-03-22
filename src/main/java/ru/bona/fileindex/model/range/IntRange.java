package ru.bona.fileindex.model.range;

import java.nio.ByteBuffer;

/**
 * IntRange
 *
 * @author Kontsur Alex (bona)
 * @since 23.09.14
 */
public class IntRange extends Range<Integer> {

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public ByteBuffer dumpRange() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(start);
        buffer.putInt(stop);
        return buffer;
    }

    @Override
    public void readRange(ByteBuffer buffer) {
        start = buffer.getInt();
        stop = buffer.getInt();
    }

    @Override
    public void setStart(Number start) {
        this.start = start.intValue();
    }

    @Override
    public void setStop(Number stop) {
        this.stop = stop.intValue();
    }

    @Override
    public int getSize() {
        return 8;
    }

}