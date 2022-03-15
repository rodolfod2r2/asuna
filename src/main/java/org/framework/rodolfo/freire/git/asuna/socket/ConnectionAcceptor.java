package org.framework.rodolfo.freire.git.asuna.socket;

import java.net.InetSocketAddress;

public interface ConnectionAcceptor {

    ConnectionAcceptor DENY = address -> false;

    ConnectionAcceptor ALLOW = address -> true;

    boolean acceptConnection(InetSocketAddress inetSocketAddress);
}
