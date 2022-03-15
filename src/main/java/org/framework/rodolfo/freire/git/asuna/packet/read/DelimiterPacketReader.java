package org.framework.rodolfo.freire.git.asuna.packet.read;

import org.framework.rodolfo.freire.git.asuna.exception.ProtocolViolationException;

import java.nio.ByteBuffer;

public class DelimiterPacketReader implements PacketReader {

    private final byte mDelimiter;
    private volatile int mMaxPacketSize;

    public DelimiterPacketReader(byte delimiter) {
        this(delimiter, -1);
    }

    public DelimiterPacketReader(byte delimiter, int maxPacketSize) {
        if (maxPacketSize < 1 && maxPacketSize != -1) {
            throw new IllegalArgumentException("Max packet size must be larger that 1, was: " + maxPacketSize);
        }
        mDelimiter = delimiter;
        mMaxPacketSize = maxPacketSize;
    }

    public int getMaxPacketSize() {
        return mMaxPacketSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        mMaxPacketSize = maxPacketSize;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public byte[] nextPacket(ByteBuffer byteBuffer) throws ProtocolViolationException {
        byteBuffer.mark();
        int bytesRead = 0;
        while (byteBuffer.remaining() > 0) {
            int ch = byteBuffer.get();
            if (ch == mDelimiter) {
                byte[] packet = new byte[bytesRead];
                byteBuffer.reset();
                byteBuffer.get(packet);
                byteBuffer.get();
                return packet;
            }
            bytesRead++;
            if (mMaxPacketSize > 0 && bytesRead > mMaxPacketSize)
                throw new ProtocolViolationException("Packet exceeds max " + mMaxPacketSize);
        }
        byteBuffer.reset();
        return null;
    }
}