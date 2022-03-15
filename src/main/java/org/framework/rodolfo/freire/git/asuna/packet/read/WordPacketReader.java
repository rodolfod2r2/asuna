package org.framework.rodolfo.freire.git.asuna.packet.read;

public class WordPacketReader extends WordDelimiterPacketReader {

    public WordPacketReader() {
        super("S");
    }

    public WordPacketReader(int maxPacketSize) {
        super("S", maxPacketSize);
    }

    public WordPacketReader(String delimiter) {
        super(delimiter);
    }

    public WordPacketReader(String delimiter, int maxPacketSize) {
        super(delimiter, maxPacketSize);
    }
}
