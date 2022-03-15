package org.framework.rodolfo.freire.git.asuna.socket.ssl;

import org.framework.rodolfo.freire.git.asuna.socket.NIOSocket;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

public interface NIOSocketSSL extends NIOSocket {

    SSLEngine getSSLEngine();

    void beginHandshake() throws SSLException;

    boolean isEncrypted();
}
