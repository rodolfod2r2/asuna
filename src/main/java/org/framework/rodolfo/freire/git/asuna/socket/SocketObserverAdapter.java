package org.framework.rodolfo.freire.git.asuna.socket;

public class SocketObserverAdapter implements SocketObserver {

    public void connectionBroken(NIOSocket nioSocket, Exception exception) {
    }

    public void packetReceived(NIOSocket socket, byte[] packet) {

    }

    public void connectionOpened(NIOSocket nioSocket) {
    }

    public void packetSent(NIOSocket socket, Object tag) {
    }
}
