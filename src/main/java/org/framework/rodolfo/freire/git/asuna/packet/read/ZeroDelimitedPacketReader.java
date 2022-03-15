package org.framework.rodolfo.freire.git.asuna.packet.read;

public class ZeroDelimitedPacketReader extends DelimiterPacketReader {

    public ZeroDelimitedPacketReader() {
        super((byte) 0);
    }

    public ZeroDelimitedPacketReader(int maxPacketSize) {
        super((byte) 0, maxPacketSize);
    }

}
