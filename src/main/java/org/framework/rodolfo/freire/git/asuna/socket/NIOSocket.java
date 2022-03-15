package org.framework.rodolfo.freire.git.asuna.socket;

import org.framework.rodolfo.freire.git.asuna.packet.read.PacketReader;
import org.framework.rodolfo.freire.git.asuna.packet.write.PacketWriter;

import java.net.Socket;

public interface NIOSocket extends NIOAbstractSocket {

    boolean write(byte[] packet);

    boolean write(byte[] packet, Object tag);

    void queue(Runnable runnable);

    long getBytesRead();

    long getBytesWritten();

    long getTimeOpen();

    long getWriteQueueSize();

    int getMaxQueueSize();

    void setMaxQueueSize(int maxQueueSize);

    void setPacketReader(PacketReader packetReader);

    void setPacketWriter(PacketWriter packetWriter);

    void listen(SocketObserver socketObserver);

    void closeAfterWrite();

    Socket socket();

}
