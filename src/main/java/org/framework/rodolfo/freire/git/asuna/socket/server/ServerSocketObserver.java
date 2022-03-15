package org.framework.rodolfo.freire.git.asuna.socket.server;

import org.framework.rodolfo.freire.git.asuna.socket.NIOSocket;

import java.io.IOException;

public interface ServerSocketObserver {

    void acceptFailed(IOException exception);

    void serverSocketDied(Exception exception);

    void newConnection(NIOSocket nioSocket);

}
