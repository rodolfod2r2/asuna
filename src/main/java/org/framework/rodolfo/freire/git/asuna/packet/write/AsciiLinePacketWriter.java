package org.framework.rodolfo.freire.git.asuna.packet.write;

public class AsciiLinePacketWriter extends DelimiterPacketWriter {

    public AsciiLinePacketWriter() {
        super((byte) '\n');
    }

}
