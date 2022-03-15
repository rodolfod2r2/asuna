package org.framework.rodolfo.freire.git.asuna.packet.read;

import org.framework.rodolfo.freire.git.asuna.exception.ProtocolViolationException;

import java.nio.ByteBuffer;

public class RawPacketReader implements PacketReader {

    public final static RawPacketReader INSTANCE = new RawPacketReader();

    private RawPacketReader() {
    }

    public byte[] nextPacket(ByteBuffer byteBuffer) throws ProtocolViolationException {
        byte[] packet = new byte[byteBuffer.remaining()];
        byteBuffer.get(packet);
        return packet;
    }

}
