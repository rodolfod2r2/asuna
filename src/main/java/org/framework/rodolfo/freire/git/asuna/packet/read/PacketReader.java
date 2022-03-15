package org.framework.rodolfo.freire.git.asuna.packet.read;

import org.framework.rodolfo.freire.git.asuna.exception.ProtocolViolationException;

import java.nio.ByteBuffer;

public interface PacketReader {

    byte[] SKIP_PACKET = new byte[0];

    byte[] nextPacket(ByteBuffer byteBuffer) throws ProtocolViolationException;

}
