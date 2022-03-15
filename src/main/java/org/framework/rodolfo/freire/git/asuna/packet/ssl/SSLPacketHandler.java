package org.framework.rodolfo.freire.git.asuna.packet.ssl;

import org.framework.rodolfo.freire.git.asuna.exception.ProtocolViolationException;
import org.framework.rodolfo.freire.git.asuna.packet.read.PacketReader;
import org.framework.rodolfo.freire.git.asuna.packet.read.RawPacketReader;
import org.framework.rodolfo.freire.git.asuna.packet.write.PacketWriter;
import org.framework.rodolfo.freire.git.asuna.packet.write.RawPacketWriter;
import org.framework.rodolfo.freire.git.asuna.socket.NIOSocket;
import org.framework.rodolfo.freire.git.asuna.socket.ssl.SSLSocketChannelResponder;
import org.framework.rodolfo.freire.git.asuna.util.NIOUtils;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SSLPacketHandler implements PacketReader, PacketWriter {

    private final static Executor TASK_HANDLER = Executors.newSingleThreadExecutor();

    private final static ThreadLocal<ByteBuffer> SSL_BUFFER = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            return ByteBuffer.allocate(64 * 1024);
        }
    };

    private final SSLEngine sslEngine;
    private final NIOSocket nioSocket;
    private final SSLSocketChannelResponder sslSocketChannelResponder;
    private PacketReader packetReader;
    private PacketWriter packetWriter;
    private ByteBuffer mPartialIncomingBuffer;
    private ByteBuffer[] mInitialOutBuffer;
    private boolean mSslInitiated;

    public SSLPacketHandler(SSLEngine engine, NIOSocket socket, SSLSocketChannelResponder responder) {
        sslEngine = engine;
        nioSocket = socket;
        mPartialIncomingBuffer = null;
        packetWriter = RawPacketWriter.INSTANCE;
        packetReader = RawPacketReader.INSTANCE;
        sslSocketChannelResponder = responder;
        mSslInitiated = false;
    }

    public PacketReader getReader() {
        return packetReader;
    }

    public void setReader(PacketReader reader) {
        packetReader = reader;
    }

    public PacketWriter getWriter() {
        return packetWriter;
    }

    public void setWriter(PacketWriter writer) {
        packetWriter = writer;
    }

    private void queueSSLTasks() {
        if (!mSslInitiated) return;
        int tasksScheduled = 0;
        Runnable task;
        while ((task = sslEngine.getDelegatedTask()) != null) {
            TASK_HANDLER.execute(task);
            tasksScheduled++;
        }
        if (tasksScheduled == 0) {
            return;
        }
        TASK_HANDLER.execute(() -> nioSocket.queue(() -> reactToHandshakeStatus(sslEngine.getHandshakeStatus())));
    }

    public byte[] nextPacket(ByteBuffer byteBuffer) throws ProtocolViolationException {
        if (!mSslInitiated) {
            return packetReader.nextPacket(byteBuffer);
        }

        try {
            ByteBuffer targetBuffer = SSL_BUFFER.get();
            targetBuffer.clear();
            SSLEngineResult result = sslEngine.unwrap(byteBuffer, targetBuffer);
            switch (result.getStatus()) {
                case BUFFER_UNDERFLOW:
                    return null;
                case BUFFER_OVERFLOW:
                    throw new ProtocolViolationException("SSL Buffer Overflow");
                case CLOSED:
                    sslSocketChannelResponder.connectionBroken(nioSocket, new EOFException("SSL Connection closed"));
                    return null;
                case OK:
            }

            reactToHandshakeStatus(result.getHandshakeStatus());

            return retrieveDecryptedPacket(targetBuffer);

        } catch (SSLException e) {

            sslSocketChannelResponder.closeDueToSSLException(e);

            return null;

        }
    }

    private void reactToHandshakeStatus(SSLEngineResult.HandshakeStatus status) {
        if (!mSslInitiated) return;
        switch (status) {
            case NOT_HANDSHAKING:
            case NEED_UNWRAP:
                break;
            case NEED_TASK:
                queueSSLTasks();
                break;
            case FINISHED:
            case NEED_WRAP:
                nioSocket.write(new byte[0]);
                break;
        }
    }

    private byte[] retrieveDecryptedPacket(ByteBuffer targetBuffer) throws ProtocolViolationException {

        targetBuffer.flip();

        mPartialIncomingBuffer = NIOUtils.join(mPartialIncomingBuffer, targetBuffer);

        if (mPartialIncomingBuffer == null || mPartialIncomingBuffer.remaining() == 0) return SKIP_PACKET;

        return packetReader.nextPacket(mPartialIncomingBuffer);

    }

    public ByteBuffer[] write(ByteBuffer[] byteBuffers) {
        if (!mSslInitiated) {
            return packetWriter.write(byteBuffers);
        }

        if (sslEngine.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            if (!NIOUtils.isEmpty(byteBuffers)) {

                mInitialOutBuffer = NIOUtils.concat(mInitialOutBuffer, packetWriter.write(byteBuffers));
                byteBuffers = new ByteBuffer[0];

            }

            ByteBuffer buffer = SSL_BUFFER.get();

            ByteBuffer[] buffers = null;

            try {

                SSLEngineResult result = null;

                while (sslEngine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                    buffer.clear();
                    result = sslEngine.wrap(byteBuffers, buffer);
                    buffer.flip();
                    buffers = NIOUtils.concat(buffers, NIOUtils.copy(buffer));
                }

                if (result == null) return null;

                if (result.getStatus() != SSLEngineResult.Status.OK)
                    throw new SSLException("Unexpectedly not ok wrapping handshake data, was " + result.getStatus());

                reactToHandshakeStatus(result.getHandshakeStatus());
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
            return buffers;
        }

        ByteBuffer buffer = SSL_BUFFER.get();
        buffer.clear();

        if (NIOUtils.isEmpty(byteBuffers)) {
            if (mInitialOutBuffer == null) return null;
        } else {
            byteBuffers = packetWriter.write(byteBuffers);
        }
        if (mInitialOutBuffer != null) {
            byteBuffers = NIOUtils.concat(mInitialOutBuffer, byteBuffers);
            mInitialOutBuffer = null;
        }

        ByteBuffer[] encrypted = null;
        while (!NIOUtils.isEmpty(byteBuffers)) {

            buffer.clear();
            try {
                sslEngine.wrap(byteBuffers, buffer);
            } catch (SSLException e) {
                throw new RuntimeException(e);
            }
            buffer.flip();

            encrypted = NIOUtils.concat(encrypted, NIOUtils.copy(buffer));

        }

        return encrypted;
    }

    public SSLEngine getSSLEngine() {
        return sslEngine;
    }

    public void begin() throws SSLException {
        sslEngine.beginHandshake();
        mSslInitiated = true;
        reactToHandshakeStatus(sslEngine.getHandshakeStatus());
    }

    public void closeEngine() {
        if (!mSslInitiated) return;
        sslEngine.closeOutbound();
        sslSocketChannelResponder.write(new byte[0]);
    }

    public boolean isEncrypted() {
        return mSslInitiated;
    }
}
