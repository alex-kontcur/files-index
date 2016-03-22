package ru.bona.fileindex.model.range;

import java.nio.ByteBuffer;

/**
 * LongRange
 *
 * @author Kontsur Alex (bona)
 * @since 23.09.14
 */
public class LongRange extends Range<Long> {

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public ByteBuffer dumpRange() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(start);
        buffer.putLong(stop);
        return buffer;
    }

    @Override
    public void readRange(ByteBuffer buffer) {
        start = buffer.getLong();
        stop = buffer.getLong();
    }

    @Override
    public void setStart(Number start) {
        this.start = start.longValue();
    }

    @Override
    public void setStop(Number stop) {
        this.stop = stop.longValue();
    }

    @Override
    public int getSize() {
        return 16;
    }

}