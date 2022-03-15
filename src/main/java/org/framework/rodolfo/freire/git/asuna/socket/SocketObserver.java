package org.framework.rodolfo.freire.git.asuna.socket;

public interface SocketObserver {

    SocketObserver NULL = new SocketObserverAdapter();

    void connectionOpened(NIOSocket nioSocket);

    void connectionBroken(NIOSocket nioSocket, Exception exception);

    void packetReceived(NIOSocket socket, byte[] packet);

    void packetSent(NIOSocket socket, Object tag);

}
