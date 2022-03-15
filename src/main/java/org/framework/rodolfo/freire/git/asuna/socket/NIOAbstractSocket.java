package org.framework.rodolfo.freire.git.asuna.socket;

import java.net.InetSocketAddress;

public interface NIOAbstractSocket {

    void close();

    InetSocketAddress getAddress();

    boolean isOpen();

    String getIp();

    int getPort();

    Object getTag();

    void setTag(Object tag);

}
