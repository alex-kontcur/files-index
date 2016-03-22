package ru.bona.fileindex.model.range;

import java.nio.ByteBuffer;

/**
 * ByteRange
 *
 * @author Kontsur Alex (bona)
 * @since 23.09.14
 */
public class ByteRange extends Range<Byte> {

    /*===========================================[ INTERFACE METHODS ]============*/

    @Override
    public ByteBuffer dumpRange() {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put(start);
        buffer.put(stop);
        return buffer;
    }

    @Override
    public void readRange(ByteBuffer buffer) {
        start = buffer.get();
        stop = buffer.get();
    }

    @Override
    public void setStart(Number start) {
        this.start = start.byteValue();
    }

    @Override
    public void setStop(Number stop) {
        this.stop = stop.byteValue();
    }

    @Override
    public int getSize() {
        return 2;
    }
}
