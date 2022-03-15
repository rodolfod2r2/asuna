package org.framework.rodolfo.freire.git.asuna.socket;

import org.framework.rodolfo.freire.git.asuna.exception.ExceptionObserver;
import org.framework.rodolfo.freire.git.asuna.socket.channel.ChannelResponder;
import org.framework.rodolfo.freire.git.asuna.socket.channel.SocketChannelResponder;
import org.framework.rodolfo.freire.git.asuna.socket.server.NIOServerSocket;
import org.framework.rodolfo.freire.git.asuna.socket.server.ServerSocketChannelResponder;
import org.framework.rodolfo.freire.git.asuna.socket.ssl.NIOServerSocketSSL;
import org.framework.rodolfo.freire.git.asuna.socket.ssl.NIOSocketSSL;
import org.framework.rodolfo.freire.git.asuna.socket.ssl.SSLServerSocketChannelResponder;
import org.framework.rodolfo.freire.git.asuna.socket.ssl.SSLSocketChannelResponder;
import org.framework.rodolfo.freire.git.asuna.util.NIOUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NIOService {

    public final static int DEFAULT_IO_BUFFER_SIZE = 64 * 1024;

    private final Selector selector;
    private final Queue<Runnable> internalEventQueue;
    private ByteBuffer sharedBuffer;
    private ExceptionObserver exceptionObserver;

    public NIOService() throws IOException {
        this(DEFAULT_IO_BUFFER_SIZE);
    }

    public NIOService(int ioBufferSize) throws IOException {
        selector = Selector.open();
        internalEventQueue = new ConcurrentLinkedQueue<>();
        exceptionObserver = ExceptionObserver.DEFAULT;
        setBufferSize(ioBufferSize);
    }

    public synchronized void selectBlocking() throws IOException {
        executeQueue();
        if (selector.select() > 0) {
            handleSelectedKeys();
        }
        executeQueue();
    }

    public synchronized void selectNonBlocking() throws IOException {
        executeQueue();
        if (selector.selectNow() > 0) {
            handleSelectedKeys();
        }
        executeQueue();
    }

    public synchronized void selectBlocking(long timeout) throws IOException {
        executeQueue();
        if (selector.select(timeout) > 0) {
            handleSelectedKeys();
        }
        executeQueue();
    }

    public NIOSocket openSocket(String host, int port) throws IOException {
        return openSocket(InetAddress.getByName(host), port);
    }

    public NIOSocket openSSLSocket(SSLEngine sslEngine, String host, int port) throws IOException {
        return openSSLSocket(sslEngine, InetAddress.getByName(host), port);
    }

    public NIOSocket openSocket(InetAddress inetAddress, int port) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        InetSocketAddress address = new InetSocketAddress(inetAddress, port);
        channel.connect(address);
        return registerSocketChannel(channel, address);
    }

    public NIOSocketSSL openSSLSocket(SSLEngine sslEngine, InetAddress inetAddress, int port) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        InetSocketAddress address = new InetSocketAddress(inetAddress, port);
        channel.connect(address);
        return new SSLSocketChannelResponder(this, registerSocketChannel(channel, address), sslEngine, true);
    }

    public NIOServerSocket openServerSocket(int port, int backlog) throws IOException {
        return openServerSocket(new InetSocketAddress(port), backlog);
    }

    public NIOServerSocketSSL openSSLServerSocket(SSLContext sslContext, int port, int backlog) throws IOException {
        return openSSLServerSocket(sslContext, new InetSocketAddress(port), backlog);
    }

    public NIOServerSocket openServerSocket(int port) throws IOException {
        return openServerSocket(port, -1);
    }

    public NIOServerSocketSSL openSSLServerSocket(SSLContext sslContext, int port) throws IOException {
        return openSSLServerSocket(sslContext, port, -1);
    }

    public NIOServerSocketSSL openSSLServerSocket(SSLContext sslContext, InetSocketAddress address, int backlog) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.socket().setReuseAddress(true);
        channel.socket().bind(address, backlog);
        channel.configureBlocking(false);
        SSLServerSocketChannelResponder channelResponder = new SSLServerSocketChannelResponder(sslContext, this, channel, address);
        queue(new RegisterChannelEvent(channelResponder));
        return channelResponder;
    }


    public NIOServerSocket openServerSocket(InetSocketAddress address, int backlog) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.socket().setReuseAddress(true);
        channel.socket().bind(address, backlog);
        channel.configureBlocking(false);
        ServerSocketChannelResponder channelResponder = new ServerSocketChannelResponder(this, channel, address);
        queue(new RegisterChannelEvent(channelResponder));
        return channelResponder;
    }


    public NIOSocket registerSocketChannel(SocketChannel socketChannel, InetSocketAddress address) throws IOException {
        socketChannel.configureBlocking(false);
        SocketChannelResponder channelResponder = new SocketChannelResponder(this, socketChannel, address);
        queue(new RegisterChannelEvent(channelResponder));
        return channelResponder;
    }

    private void executeQueue() {
        Runnable event;
        while ((event = internalEventQueue.poll()) != null) {
            try {
                event.run();
            } catch (Throwable t) {
                notifyException(t);
            }
        }
    }

    private void handleSelectedKeys() {
        for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
            SelectionKey key = it.next();
            it.remove();
            try {
                handleKey(key);
            } catch (Throwable t) {
                notifyException(t);
            }
        }
    }

    public int getBufferSize() {
        return sharedBuffer.capacity();
    }

    public void setBufferSize(int newBufferSize) {
        if (newBufferSize < 256) throw new IllegalArgumentException("The buffer must at least hold 256 bytes");
        sharedBuffer = ByteBuffer.allocate(newBufferSize);
    }

    public ByteBuffer getSharedBuffer() {
        return sharedBuffer;
    }

    private void handleKey(SelectionKey key) {
        ChannelResponder responder = (ChannelResponder) key.attachment();
        try {
            if (key.isReadable()) {
                responder.socketReadyForRead();
            }
            if (key.isWritable()) {
                responder.socketReadyForWrite();
            }
            if (key.isAcceptable()) {
                responder.socketReadyForAccept();
            }
            if (key.isConnectable()) {
                responder.socketReadyForConnect();
            }
        } catch (CancelledKeyException e) {
            responder.close(e);
        }
    }

    public void close() {
        if (!isOpen()) return;
        queue(new ShutdownEvent());
    }


    public boolean isOpen() {
        return selector.isOpen();
    }

    public void queue(Runnable event) {
        internalEventQueue.add(event);
        wakeup();
    }

    public Queue<Runnable> getQueue() {
        return new LinkedList<>(internalEventQueue);
    }

    public void wakeup() {
        selector.wakeup();
    }

    public void setExceptionObserver(ExceptionObserver exceptionObserver) {
        final ExceptionObserver newExceptionObserver = exceptionObserver == null ? ExceptionObserver.DEFAULT : exceptionObserver;
        queue(() -> NIOService.this.exceptionObserver = newExceptionObserver);
    }

    public void notifyException(Throwable t) {
        try {
            exceptionObserver.notifyExceptionThrown(t);
        } catch (Exception e) {
            System.err.println("Failed to log the following exception to the exception observer:");
            System.err.println(e);
            e.printStackTrace();
        }
    }

    private class RegisterChannelEvent implements Runnable {
        private final ChannelResponder mChannelResponder;

        private RegisterChannelEvent(ChannelResponder channelResponder) {
            mChannelResponder = channelResponder;
        }

        public void run() {
            try {
                SelectionKey key = mChannelResponder.getChannel().register(selector, mChannelResponder.getChannel().validOps());
                mChannelResponder.setKey(key);
                key.attach(mChannelResponder);
            } catch (Exception e) {
                mChannelResponder.close(e);
            }
        }

        @Override
        public String toString() {
            return "Register[" + mChannelResponder + "]";
        }
    }

    private class ShutdownEvent implements Runnable {
        public void run() {
            if (!isOpen()) return;
            for (SelectionKey key : selector.keys()) {
                try {
                    NIOUtils.cancelKeySilently(key);
                    ((ChannelResponder) key.attachment()).close();
                } catch (Exception e) {
                    // Swallow exceptions.
                }
            }
            try {
                selector.close();
            } catch (IOException e) {
                // Swallow exceptions.
            }
        }
    }
}
