package org.framework.rodolfo.freire.git.asuna.packet.read;

import org.framework.rodolfo.freire.git.asuna.exception.ProtocolViolationException;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import java.nio.ByteBuffer;

public class StreamCipherPacketReader implements PacketReader {

    private final Cipher mCipher;
    private final PacketReader mReader;
    private ByteBuffer mInternalBuffer;

    public StreamCipherPacketReader(Cipher cipher, PacketReader reader) {
        mCipher = cipher;
        mReader = reader;
    }

    public byte[] nextPacket(ByteBuffer byteBuffer) throws ProtocolViolationException {
        if (mInternalBuffer == null) {
            mInternalBuffer = ByteBuffer.allocate(mCipher.getOutputSize(byteBuffer.remaining()));
        } else {
            ByteBuffer newBuffer = ByteBuffer.allocate(mCipher.getOutputSize(byteBuffer.remaining()) + mInternalBuffer.remaining());
            newBuffer.put(mInternalBuffer);
            mInternalBuffer = newBuffer;
        }
        try {
            int consumed = mCipher.update(byteBuffer, mInternalBuffer);
        } catch (ShortBufferException e) {
            throw new ProtocolViolationException("Short buffer");
        }
        mInternalBuffer.flip();
        byte[] packet = mReader.nextPacket(mInternalBuffer);
        if (mInternalBuffer.remaining() == 0) mInternalBuffer = null;
        return packet;
    }

}
