package org.framework.rodolfo.freire.git.asuna.socket.reader;

import org.framework.rodolfo.freire.git.asuna.socket.NIOService;
import org.framework.rodolfo.freire.git.asuna.util.NIOUtils;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketReader {

    private final NIOService nioService;
    private ByteBuffer previousBytes;
    private long newBytesRead;

    public SocketReader(NIOService nioService) {
        this.nioService = nioService;
        newBytesRead = 0;
    }

    public int read(SocketChannel channel) throws IOException {

        ByteBuffer buffer = getBuffer();
        buffer.clear();

        if (previousBytes != null) {
            buffer.position(previousBytes.remaining());
        }

        int read = channel.read(buffer);

        if (read < 0) throw new EOFException("Buffer read -1");

        if (!buffer.hasRemaining()) throw new BufferOverflowException();

        newBytesRead += read;

        if (read == 0) return 0;

        if (previousBytes != null) {
            int position = buffer.position();
            buffer.position(0);
            buffer.put(previousBytes);
            buffer.position(position);
            previousBytes = null;
        }

        buffer.flip();

        return read;
    }

    public void compact() {

        ByteBuffer buffer = getBuffer();

        if (buffer.remaining() > 0) {
            previousBytes = NIOUtils.copy(buffer);
        }

    }

    public long getBytesRead() {
        return newBytesRead;
    }

    public ByteBuffer getBuffer() {
        return nioService.getSharedBuffer();
    }
}
