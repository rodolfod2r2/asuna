package org.framework.rodolfo.freire.git.asuna.packet.write;

import org.framework.rodolfo.freire.git.asuna.util.NIOUtils;

import java.nio.ByteBuffer;


public class RegularPacketWriter implements PacketWriter {

    private final boolean m_bigEndian;
    private final ByteBuffer m_header;

    public RegularPacketWriter(int headerSize, boolean bigEndian) {
        if (headerSize < 1 || headerSize > 4)
            throw new IllegalArgumentException("Header must be between 1 and 4 bytes long.");
        m_bigEndian = bigEndian;
        m_header = ByteBuffer.allocate(headerSize);
    }

    public ByteBuffer[] write(ByteBuffer[] byteBuffers) {
        m_header.clear();
        NIOUtils.setPacketSizeInByteBuffer(m_header, m_header.capacity(), (int) NIOUtils.remaining(byteBuffers), m_bigEndian);
        m_header.flip();
        return NIOUtils.concat(m_header, byteBuffers);
    }

}
