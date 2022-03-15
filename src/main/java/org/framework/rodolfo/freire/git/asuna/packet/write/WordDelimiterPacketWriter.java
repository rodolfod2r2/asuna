package org.framework.rodolfo.freire.git.asuna.packet.write;

import org.framework.rodolfo.freire.git.asuna.util.NIOUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class WordDelimiterPacketWriter implements PacketWriter {

    private final ByteBuffer mEndByte;

    public WordDelimiterPacketWriter(String mEndByte) {
        this.mEndByte = ByteBuffer.wrap(mEndByte.getBytes(StandardCharsets.UTF_8));;
    }

    public ByteBuffer[] write(ByteBuffer[] byteBuffer) {
        mEndByte.rewind();
        return NIOUtils.concat(byteBuffer, mEndByte);
    }
}
