package org.framework.rodolfo.freire.git.asuna.packet.write;

import org.framework.rodolfo.freire.git.asuna.util.NIOUtils;

import java.nio.ByteBuffer;

public class DelimiterPacketWriter implements PacketWriter {

    private final ByteBuffer mEndByte;

    public DelimiterPacketWriter(byte endByte) {
        mEndByte = ByteBuffer.wrap(new byte[]{endByte});
    }

    public ByteBuffer[] write(ByteBuffer[] byteBuffer) {
        mEndByte.rewind();
        return NIOUtils.concat(byteBuffer, mEndByte);
    }
}
