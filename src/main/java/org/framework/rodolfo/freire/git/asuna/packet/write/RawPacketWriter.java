package org.framework.rodolfo.freire.git.asuna.packet.write;

import java.nio.ByteBuffer;

public class RawPacketWriter implements PacketWriter {

    public static RawPacketWriter INSTANCE = new RawPacketWriter();

    private RawPacketWriter() {
    }

    public ByteBuffer[] write(ByteBuffer[] byteBuffers) {
        return byteBuffers;
    }
}
