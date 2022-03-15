package org.framework.rodolfo.freire.git.asuna.socket.server;

import org.framework.rodolfo.freire.git.asuna.socket.ConnectionAcceptor;
import org.framework.rodolfo.freire.git.asuna.socket.NIOAbstractSocket;

import java.net.ServerSocket;

public interface NIOServerSocket extends NIOAbstractSocket {

    long getTotalConnections();

    long getTotalRefusedConnections();

    long getTotalAcceptedConnections();

    long getTotalFailedConnections();

    void listen(ServerSocketObserver observer);

    void setConnectionAcceptor(ConnectionAcceptor acceptor);

    ServerSocket socket();

}
