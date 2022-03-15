package org.framework.rodolfo.freire.git.asuna.packet.write;

public class WordPacketWriter extends WordDelimiterPacketWriter {

    public WordPacketWriter(String mEndByte) {
        super(mEndByte);
    }

    public WordPacketWriter() {
        super("S");
    }


}
