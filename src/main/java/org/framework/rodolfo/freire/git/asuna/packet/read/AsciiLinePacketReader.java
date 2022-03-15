package org.framework.rodolfo.freire.git.asuna.packet.read;

public class AsciiLinePacketReader extends DelimiterPacketReader {

    public AsciiLinePacketReader() {
        super((byte) '\n');
    }

    public AsciiLinePacketReader(int maxLineLength) {
        super((byte) '\n', maxLineLength);
    }

}
