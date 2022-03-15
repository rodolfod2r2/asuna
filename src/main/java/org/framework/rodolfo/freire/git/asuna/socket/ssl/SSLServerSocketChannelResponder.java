package org.framework.rodolfo.freire.git.asuna.socket.ssl;

import org.framework.rodolfo.freire.git.asuna.socket.NIOService;
import org.framework.rodolfo.freire.git.asuna.socket.NIOSocket;
import org.framework.rodolfo.freire.git.asuna.socket.server.ServerSocketChannelResponder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SSLServerSocketChannelResponder extends ServerSocketChannelResponder implements NIOServerSocketSSL {

    private final SSLContext m_sslContext;

    public SSLServerSocketChannelResponder(SSLContext context, NIOService service, ServerSocketChannel channel, InetSocketAddress address) throws IOException {
        super(service, channel, address);
        m_sslContext = context;
    }

    public SSLContext getSSLContext() {
        return m_sslContext;
    }

    @Override
    public NIOSocket registerSocket(SocketChannel channel, InetSocketAddress address) throws IOException {
        NIOSocket socket = super.registerSocket(channel, address);
        return new SSLSocketChannelResponder(getNIOService(), socket, m_sslContext.createSSLEngine(), false);
    }
}
