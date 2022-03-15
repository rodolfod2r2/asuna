package org.framework.rodolfo.freire.git.asuna.packet.read;

import lombok.extern.slf4j.Slf4j;
import org.framework.rodolfo.freire.git.asuna.exception.ProtocolViolationException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Slf4j
public class WordDelimiterPacketReader implements PacketReader {

    private final String mDelimiter;
    private volatile int mMaxPacketSize;


    public WordDelimiterPacketReader(String delimiter) {
        this(delimiter, -1);
    }

    public WordDelimiterPacketReader(String delimiter, int maxPacketSize) {
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

    public byte[] nextPacket(ByteBuffer byteBuffer) throws ProtocolViolationException {
        byteBuffer.mark();
        int bytesRead = 0;
        while (byteBuffer.remaining() > 0) {
            String s = StandardCharsets.UTF_8.decode(byteBuffer).toString();
            if (!s.isEmpty()) {
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