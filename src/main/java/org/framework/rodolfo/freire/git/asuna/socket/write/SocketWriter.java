package org.framework.rodolfo.freire.git.asuna.socket.write;

import org.framework.rodolfo.freire.git.asuna.packet.write.PacketWriter;
import org.framework.rodolfo.freire.git.asuna.packet.write.RawPacketWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketWriter {

    private long bytesWritten;
    private ByteBuffer[] byteBuffers;
    private PacketWriter packetWriter;
    private Object mTag;
    private int currentBuffer;

    public SocketWriter() {
        bytesWritten = 0;
        byteBuffers = null;
        packetWriter = RawPacketWriter.INSTANCE;
    }

    public PacketWriter getPacketWriter() {
        return packetWriter;
    }

    public void setPacketWriter(PacketWriter packetWriter) {
        this.packetWriter = packetWriter;
    }

    public boolean isEmpty() {
        return byteBuffers == null;
    }

    public void setPacket(byte[] data, Object tag) {
        if (!isEmpty()) throw new IllegalStateException("This method should only called when m_writeBuffers == null");
        byteBuffers = packetWriter.write(new ByteBuffer[]{ByteBuffer.wrap(data)});
        currentBuffer = 0;
        mTag = tag;
    }

    public boolean write(SocketChannel channel) throws IOException {
        if (byteBuffers == null || (currentBuffer == byteBuffers.length - 1 && !byteBuffers[currentBuffer].hasRemaining())) {
            byteBuffers = null;
            return true;
        }

        long written = channel.write(byteBuffers, currentBuffer, byteBuffers.length - currentBuffer);

        if (written == 0) return false;

        bytesWritten += written;

        for (int i = currentBuffer; i < byteBuffers.length; i++) {
            if (byteBuffers[i].hasRemaining()) {
                currentBuffer = i;
                break;
            }
            byteBuffers[i] = null;
        }

        if (byteBuffers[currentBuffer] == null) {
            byteBuffers = null;
        }

        return true;

    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public Object getTag() {
        return mTag;
    }
}
