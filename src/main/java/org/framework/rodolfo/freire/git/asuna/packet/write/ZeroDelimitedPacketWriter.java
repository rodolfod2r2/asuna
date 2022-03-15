package org.framework.rodolfo.freire.git.asuna.packet.write;

public class ZeroDelimitedPacketWriter extends DelimiterPacketWriter {

    public ZeroDelimitedPacketWriter() {
        super((byte) 0);
    }

}
