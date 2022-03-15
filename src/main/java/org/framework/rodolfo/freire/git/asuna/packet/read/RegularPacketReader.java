package org.framework.rodolfo.freire.git.asuna.packet.read;

import org.framework.rodolfo.freire.git.asuna.exception.ProtocolViolationException;
import org.framework.rodolfo.freire.git.asuna.util.NIOUtils;

import java.nio.ByteBuffer;

public class RegularPacketReader implements PacketReader {

    private final boolean m_bigEndian;
    private final int m_headerSize;

    public RegularPacketReader(int headerSize, boolean bigEndian) {
        if (headerSize < 1 || headerSize > 4)
            throw new IllegalArgumentException("Header must be between 1 and 4 bytes long.");
        m_bigEndian = bigEndian;
        m_headerSize = headerSize;
    }

    public byte[] nextPacket(ByteBuffer byteBuffer) throws ProtocolViolationException {
        if (byteBuffer.remaining() < m_headerSize) return null;
        byteBuffer.mark();
        int length = NIOUtils.getPacketSizeFromByteBuffer(byteBuffer, m_headerSize, m_bigEndian);
        if (byteBuffer.remaining() >= length) {
            byte[] packet = new byte[length];
            byteBuffer.get(packet);
            return packet;
        } else {
            byteBuffer.reset();
            return null;
        }
    }

}
