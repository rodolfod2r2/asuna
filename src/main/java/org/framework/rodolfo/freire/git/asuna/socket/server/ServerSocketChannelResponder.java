package org.framework.rodolfo.freire.git.asuna.socket.server;

import org.framework.rodolfo.freire.git.asuna.socket.ConnectionAcceptor;
import org.framework.rodolfo.freire.git.asuna.socket.NIOService;
import org.framework.rodolfo.freire.git.asuna.socket.NIOSocket;
import org.framework.rodolfo.freire.git.asuna.socket.channel.ChannelResponder;
import org.framework.rodolfo.freire.git.asuna.util.NIOUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ServerSocketChannelResponder extends ChannelResponder implements NIOServerSocket {

    private long totalRefusedConnections;
    private long totalAcceptedConnections;
    private long totalFailedConnections;
    private long totalConnections;
    private volatile ConnectionAcceptor connectionAcceptor;
    private ServerSocketObserver serverSocketObserver;

    @SuppressWarnings({"ObjectToString"})
    public ServerSocketChannelResponder(NIOService service, ServerSocketChannel channel, InetSocketAddress address) throws IOException {
        super(service, channel, address);
        serverSocketObserver = null;
        setConnectionAcceptor(ConnectionAcceptor.ALLOW);
        totalRefusedConnections = 0;
        totalAcceptedConnections = 0;
        totalFailedConnections = 0;
        totalConnections = 0;
    }

    public void keyInitialized() {
        addInterest(SelectionKey.OP_ACCEPT);
    }

    public ServerSocketChannel getChannel() {
        return (ServerSocketChannel) super.getChannel();
    }

    public NIOSocket registerSocket(SocketChannel channel, InetSocketAddress address) throws IOException {
        return getNIOService().registerSocketChannel(channel, address);
    }

    private void notifyNewConnection(NIOSocket socket) {
        try {
            if (serverSocketObserver != null) serverSocketObserver.newConnection(socket);
        } catch (Exception e) {
            getNIOService().notifyException(e);
            socket.close();
        }
    }

    private void notifyAcceptFailed(IOException theException) {
        try {
            if (serverSocketObserver != null) serverSocketObserver.acceptFailed(theException);
        } catch (Exception e) {
            getNIOService().notifyException(e);
        }
    }

    public void socketReadyForAccept() {
        totalConnections++;
        SocketChannel socketChannel = null;
        try {
            socketChannel = getChannel().accept();
            if (socketChannel == null) {
                totalConnections--;
                return;
            }

            InetSocketAddress address = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();

            if (!connectionAcceptor.acceptConnection(address)) {
                totalRefusedConnections++;
                NIOUtils.closeChannelSilently(socketChannel);
                return;
            }

            notifyNewConnection(registerSocket(socketChannel, address));

            totalAcceptedConnections++;

        } catch (IOException e) {

            NIOUtils.closeChannelSilently(socketChannel);
            totalFailedConnections++;
            notifyAcceptFailed(e);

        }
    }

    public void notifyWasCancelled() {
        close();
    }

    public long getTotalRefusedConnections() {
        return totalRefusedConnections;
    }

    public long getTotalConnections() {
        return totalConnections;
    }

    public long getTotalFailedConnections() {
        return totalFailedConnections;
    }

    public long getTotalAcceptedConnections() {
        return totalAcceptedConnections;
    }

    public void setConnectionAcceptor(ConnectionAcceptor connectionAcceptor) {
        this.connectionAcceptor = connectionAcceptor == null ? ConnectionAcceptor.DENY : connectionAcceptor;
    }

    private void notifyObserverSocketDied(Exception exception) {
        try {
            if (serverSocketObserver != null) serverSocketObserver.serverSocketDied(exception);
        } catch (Exception e) {
            getNIOService().notifyException(e);
        }

    }

    public void listen(ServerSocketObserver observer) {
        if (observer == null) throw new NullPointerException();
        markObserverSet();
        getNIOService().queue(new BeginListenEvent(observer));
    }

    public void shutdown(Exception e) {
        notifyObserverSocketDied(e);
    }

    public ServerSocket socket() {
        return getChannel().socket();
    }

    private class BeginListenEvent implements Runnable {
        private final ServerSocketObserver m_newObserver;

        private BeginListenEvent(ServerSocketObserver socketObserver) {
            m_newObserver = socketObserver;
        }

        public void run() {
            serverSocketObserver = m_newObserver;
            if (!isOpen()) {
                notifyObserverSocketDied(null);
                return;
            }
            addInterest(SelectionKey.OP_ACCEPT);
        }

        @Override
        public String toString() {
            return "BeginListen[" + m_newObserver + "]";
        }
    }
}
