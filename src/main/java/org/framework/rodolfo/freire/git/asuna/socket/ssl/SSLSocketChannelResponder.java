package org.framework.rodolfo.freire.git.asuna.socket.ssl;

import org.framework.rodolfo.freire.git.asuna.packet.read.PacketReader;
import org.framework.rodolfo.freire.git.asuna.packet.ssl.SSLPacketHandler;
import org.framework.rodolfo.freire.git.asuna.packet.write.PacketWriter;
import org.framework.rodolfo.freire.git.asuna.socket.NIOService;
import org.framework.rodolfo.freire.git.asuna.socket.NIOSocket;
import org.framework.rodolfo.freire.git.asuna.socket.SocketObserver;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SSLSocketChannelResponder implements NIOSocketSSL, SocketObserver {

    private final NIOSocket nioSocket;
    private final SSLPacketHandler sslPacketHandler;
    private final NIOService nioService;
    private SocketObserver socketObserver;

    public SSLSocketChannelResponder(NIOService nioService, NIOSocket wrappedSocket, SSLEngine engine, boolean client) throws SSLException {
        this.nioService = nioService;
        nioSocket = wrappedSocket;
        sslPacketHandler = new SSLPacketHandler(engine, nioSocket, this);
        nioSocket.setPacketReader(sslPacketHandler);
        nioSocket.setPacketWriter(sslPacketHandler);
        engine.setUseClientMode(client);
    }

    public void beginHandshake() throws SSLException {
        if (getSSLEngine().getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
            throw new IllegalStateException("Tried to start handshake during handshake.");
        sslPacketHandler.begin();
    }

    public boolean isEncrypted() {
        return sslPacketHandler.isEncrypted();
    }

    public SSLEngine getSSLEngine() {
        return sslPacketHandler.getSSLEngine();
    }


    public boolean write(byte[] packet) {
        return nioSocket.write(packet);
    }

    public boolean write(byte[] packet, Object tag) {
        return nioSocket.write(packet, tag);
    }

    public void queue(Runnable runnable) {
        nioSocket.queue(runnable);
    }

    public long getBytesRead() {
        return nioSocket.getBytesRead();
    }

    public long getBytesWritten() {
        return nioSocket.getBytesWritten();
    }

    public long getTimeOpen() {
        return nioSocket.getTimeOpen();
    }

    public long getWriteQueueSize() {
        return nioSocket.getWriteQueueSize();
    }

    public int getMaxQueueSize() {
        return nioSocket.getMaxQueueSize();
    }

    public void setMaxQueueSize(int maxQueueSize) {
        nioSocket.setMaxQueueSize(maxQueueSize);
    }

    public void setPacketReader(PacketReader packetReader) {
        sslPacketHandler.setReader(packetReader);
    }

    public void setPacketWriter(final PacketWriter packetWriter) {
        nioSocket.queue(() -> sslPacketHandler.setWriter(packetWriter));
    }

    public void listen(SocketObserver socketObserver) {
        this.socketObserver = socketObserver;
        nioSocket.listen(this);
    }

    public void closeAfterWrite() {
        sslPacketHandler.closeEngine();
        nioSocket.closeAfterWrite();
    }

    public Socket socket() {
        return nioSocket.socket();
    }

    public void close() {
        nioSocket.close();
    }

    public InetSocketAddress getAddress() {
        return nioSocket.getAddress();
    }

    public boolean isOpen() {
        return nioSocket.isOpen();
    }

    public String getIp() {
        return nioSocket.getIp();
    }

    public int getPort() {
        return nioSocket.getPort();
    }

    public Object getTag() {
        return nioSocket.getTag();
    }

    public void setTag(Object tag) {
        nioSocket.setTag(tag);
    }

    public void closeDueToSSLException(SSLException e) {
        try {
            if (socketObserver != null) socketObserver.connectionBroken(this, e);
        } catch (Exception ex) {
            nioService.notifyException(e);
        }
        nioSocket.close();
    }

    public void connectionOpened(NIOSocket nioSocket) {
        try {
            if (socketObserver != null) socketObserver.connectionOpened(this);
        } catch (Exception e) {
            nioService.notifyException(e);
        }
    }

    public void connectionBroken(NIOSocket nioSocket, Exception exception) {
        try {
            if (socketObserver != null) socketObserver.connectionBroken(this, exception);
        } catch (Exception e) {
            nioService.notifyException(e);
        }
    }

    public void packetReceived(NIOSocket socket, byte[] packet) {
        try {
            if (socketObserver != null) socketObserver.packetReceived(this, packet);
        } catch (Exception e) {
            nioService.notifyException(e);
        }
    }

    public void packetSent(NIOSocket socket, Object tag) {
        try {
            if (socketObserver != null) socketObserver.packetSent(this, tag);
        } catch (Exception e) {
            nioService.notifyException(e);
        }
    }

}
