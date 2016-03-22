package ru.bona.fileindex;

import org.junit.Assert;
import org.junit.Test;
import ru.bona.fileindex.channelworker.bytes.BytesChannel;

import java.nio.ByteBuffer;

/**
 * BsChannelTest
 *
 * @author Kontsur Alex (bona)
 * @since 05.10.14
 */
public class BsChannelTest {

    @Test
    public void cutFromChannelWorksCorrectly() throws Exception {
        BytesChannel channel = new BytesChannel();

        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        channel.write(buffer);
        buffer = ByteBuffer.allocate(2);
        buffer.put((byte) 3);
        buffer.put((byte) 4);
        channel.write(buffer);

        channel = (BytesChannel) channel.cut(1, 2);

        buffer = ByteBuffer.allocate(2);
        channel.read(buffer);
        buffer.flip();
        Assert.assertEquals(1, buffer.get());
        Assert.assertEquals(4, buffer.get());
    }

    @Test
    public void bsChannelReadSameBytesAsWasWritten() throws Exception {
        BytesChannel channel = new BytesChannel();

        ByteBuffer buffer = ByteBuffer.allocate(3);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 3);
        channel.write(buffer);
        buffer = ByteBuffer.allocate(2);
        buffer.put((byte) 4);
        buffer.put((byte) 5);
        channel.write(buffer);

        buffer = ByteBuffer.allocate(2);
        channel.read(buffer);
        buffer.flip();

        Assert.assertEquals(1, buffer.get());
        Assert.assertEquals(2, buffer.get());

        buffer = ByteBuffer.allocate(2);
        channel.read(buffer);
        buffer.flip();
        Assert.assertEquals(3, buffer.get());
        Assert.assertEquals(4, buffer.get());
    }
}
