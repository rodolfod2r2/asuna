package org.framework.rodolfo.freire.git.asuna.packet.write;

import java.nio.ByteBuffer;

public interface PacketWriter {
    ByteBuffer[] write(ByteBuffer[] byteBuffer);
}
