package org.framework.rodolfo.freire.git.asuna.socket.ssl;

import org.framework.rodolfo.freire.git.asuna.socket.server.NIOServerSocket;

import javax.net.ssl.SSLContext;

public interface NIOServerSocketSSL extends NIOServerSocket {

    SSLContext getSSLContext();

}
