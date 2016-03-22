package ru.bona.fileindex.model.range;

import java.nio.ByteBuffer;

/**
 * ShortRange
 *
 * @author Kontsur Alex (bona)
 * @since 23.09.14
 */
public class ShortRange extends Range<Short> {

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public ByteBuffer dumpRange() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putShort(start);
        buffer.putShort(stop);
        return buffer;
    }

    @Override
    public void readRange(ByteBuffer buffer) {
        start = buffer.getShort();
        stop = buffer.getShort();
    }

    @Override
    public void setStart(Number start) {
        this.start = start.shortValue();
    }

    @Override
    public void setStop(Number stop) {
        this.stop = stop.shortValue();
    }

    @Override
    public int getSize() {
        return 4;
    }

}
