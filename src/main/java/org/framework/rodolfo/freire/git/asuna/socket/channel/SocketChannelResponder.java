package org.framework.rodolfo.freire.git.asuna.socket.channel;

import org.framework.rodolfo.freire.git.asuna.packet.read.PacketReader;
import org.framework.rodolfo.freire.git.asuna.packet.read.RawPacketReader;
import org.framework.rodolfo.freire.git.asuna.packet.write.PacketWriter;
import org.framework.rodolfo.freire.git.asuna.socket.NIOService;
import org.framework.rodolfo.freire.git.asuna.socket.NIOSocket;
import org.framework.rodolfo.freire.git.asuna.socket.SocketObserver;
import org.framework.rodolfo.freire.git.asuna.socket.reader.SocketReader;
import org.framework.rodolfo.freire.git.asuna.socket.write.SocketWriter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class SocketChannelResponder extends ChannelResponder implements NIOSocket {

    private final AtomicLong bytesInQueue;
    private final SocketReader socketReader;
    private final SocketWriter socketWriter;
    private int maxQueueSize;
    private long timeOpened;
    private final ConcurrentLinkedQueue<Object> packetQueue;
    private PacketReader packetReader;
    private volatile SocketObserver socketObserver;

    public SocketChannelResponder(NIOService service, SocketChannel socketChannel, InetSocketAddress address) {
        super(service, socketChannel, address);
        socketObserver = null;
        maxQueueSize = -1;
        timeOpened = -1;
        packetReader = RawPacketReader.INSTANCE;
        bytesInQueue = new AtomicLong(0L);
        packetQueue = new ConcurrentLinkedQueue<Object>();
        socketReader = new SocketReader(service);
        socketWriter = new SocketWriter();
    }

    public void keyInitialized() {
        if (!isConnected()) {
            addInterest(SelectionKey.OP_CONNECT);
        }
    }

    public void closeAfterWrite() {
        queue(() -> {
            packetQueue.clear();
            close(null);
        });
    }

    public void queue(Runnable runnable) {
        packetQueue.offer(runnable);
        getNIOService().queue(new AddInterestEvent(SelectionKey.OP_WRITE));
    }

    public boolean write(byte[] packet, Object tag) {
        long currentQueueSize = bytesInQueue.addAndGet(packet.length);
        if (maxQueueSize > 0 && currentQueueSize > maxQueueSize) {
            bytesInQueue.addAndGet(-packet.length);
            return false;
        }

        packetQueue.offer(tag == null ? packet : new Object[]{packet, tag});
        getNIOService().queue(new AddInterestEvent(SelectionKey.OP_WRITE));

        return true;
    }

    public boolean write(byte[] packet) {
        return write(packet, null);
    }

    public boolean isConnected() {
        return getChannel().isConnected();
    }

    private void notifyPacketReceived(byte[] packet) {
        try {
            if (socketObserver != null) socketObserver.packetReceived(this, packet);
        } catch (Exception e) {
            getNIOService().notifyException(e);
        }
    }

    private void notifyPacketSent(Object tag) {
        try {
            if (socketObserver != null) socketObserver.packetSent(this, tag);
        } catch (Exception e) {
            getNIOService().notifyException(e);
        }
    }

    public void socketReadyForRead() {
        if (!isOpen()) return;
        try {
            if (!isConnected()) throw new IOException("Channel not connected.");
            while (socketReader.read(getChannel()) > 0) {
                byte[] packet;
                ByteBuffer buffer = socketReader.getBuffer();
                while (buffer.remaining() > 0
                        && (packet = packetReader.nextPacket(buffer)) != null) {
                    if (packet == PacketReader.SKIP_PACKET) continue;
                    notifyPacketReceived(packet);
                }
                socketReader.compact();
            }
        } catch (Exception e) {
            close(e);
        }
    }

    private void fillCurrentOutgoingBuffer() throws IOException {
        if (socketWriter.isEmpty()) {
            // Retrieve next packet from the queue.
            Object nextPacket = packetQueue.poll();
            while (nextPacket != null && nextPacket instanceof Runnable) {
                ((Runnable) nextPacket).run();
                nextPacket = packetQueue.poll();
            }
            if (nextPacket == null) return;
            byte[] data;
            Object tag = null;
            if (nextPacket instanceof byte[]) {
                data = (byte[]) nextPacket;
            } else {
                data = (byte[]) ((Object[]) nextPacket)[0];
                tag = ((Object[]) nextPacket)[1];
            }
            socketWriter.setPacket(data, tag);
            bytesInQueue.addAndGet(-data.length);
        }
    }

    public void socketReadyForWrite() {
        try {
            deleteInterest(SelectionKey.OP_WRITE);
            if (!isOpen()) return;
            fillCurrentOutgoingBuffer();
            if (socketWriter.isEmpty()) {
                return;
            }
            while (!socketWriter.isEmpty()) {
                boolean bytesWereWritten = socketWriter.write(getChannel());
                if (!bytesWereWritten) {
                    addInterest(SelectionKey.OP_WRITE);
                    return;
                }
                if (socketWriter.isEmpty()) {
                    notifyPacketSent(socketWriter.getTag());
                    fillCurrentOutgoingBuffer();
                }
            }
        } catch (Exception e) {
            close(e);
        }
    }

    public void socketReadyForConnect() {
        try {
            if (!isOpen()) return;
            if (getChannel().finishConnect()) {
                deleteInterest(SelectionKey.OP_CONNECT);
                timeOpened = System.currentTimeMillis();
                notifyObserverOfConnect();
            }

        } catch (Exception e) {
            close(e);
        }
    }

    public void notifyWasCancelled() {
        close();
    }

    public Socket getSocket() {
        return getChannel().socket();
    }

    public long getBytesRead() {
        return socketReader.getBytesRead();
    }

    public long getBytesWritten() {
        return socketWriter.getBytesWritten();
    }

    public long getTimeOpen() {
        return timeOpened > 0 ? System.currentTimeMillis() - timeOpened : -1;
    }

    public long getWriteQueueSize() {
        return bytesInQueue.get();
    }

    public String toString() {
        try {
            return getSocket().toString();
        } catch (Exception e) {
            return "Closed NIO Socket";
        }
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    public void listen(SocketObserver socketObserver) {
        markObserverSet();
        getNIOService().queue(new beginListenEvent(this, socketObserver == null ? SocketObserver.NULL : socketObserver));
    }

    private void notifyObserverOfConnect() {
        try {
            if (socketObserver != null) socketObserver.connectionOpened(this);
        } catch (Exception e) {
            getNIOService().notifyException(e);
        }
    }

    private void notifyObserverOfDisconnect(Exception exception) {
        try {
            if (socketObserver != null) socketObserver.connectionBroken(this, exception);
        } catch (Exception e) {
            getNIOService().notifyException(e);
        }
    }

    public void setPacketReader(PacketReader packetReader) {
        this.packetReader = packetReader;
    }

    public void setPacketWriter(final PacketWriter packetWriter) {
        if (packetWriter == null) throw new NullPointerException();
        queue(() -> socketWriter.setPacketWriter(packetWriter));
    }

    public SocketChannel getChannel() {
        return (SocketChannel) super.getChannel();
    }

    public void shutdown(Exception e) {
        timeOpened = -1;
        packetQueue.clear();
        bytesInQueue.set(0);
        notifyObserverOfDisconnect(e);
    }

    public Socket socket() {
        return getChannel().socket();
    }

    private class AddInterestEvent implements Runnable {
        private final int m_interest;

        private AddInterestEvent(int interest) {
            m_interest = interest;
        }

        public void run() {
            addInterest(m_interest);
        }
    }

    private static class beginListenEvent implements Runnable {
        private final SocketObserver newObserver;
        private final SocketChannelResponder socketChannelResponder;

        private beginListenEvent(SocketChannelResponder responder, SocketObserver socketObserver) {
            socketChannelResponder = responder;
            newObserver = socketObserver;
        }

        public void run() {
            socketChannelResponder.socketObserver = newObserver;
            if (socketChannelResponder.isConnected()) {
                socketChannelResponder.notifyObserverOfConnect();
            }
            if (!socketChannelResponder.isOpen()) {
                socketChannelResponder.notifyObserverOfDisconnect(null);
            }
            socketChannelResponder.addInterest(SelectionKey.OP_READ);
        }

        @Override
        public String toString() {
            return "BeginListen[" + newObserver + "]";
        }
    }
}
