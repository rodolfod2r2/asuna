package org.framework.rodolfo.freire.git.asuna.socket.server;

import org.framework.rodolfo.freire.git.asuna.socket.NIOSocket;

import java.io.IOException;

public class ServerSocketObserverAdapter implements ServerSocketObserver {

    public void acceptFailed(IOException exception) {
    }

    public void serverSocketDied(Exception e) {
    }

    public void newConnection(NIOSocket nioSocket) {
    }
}
