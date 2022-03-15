package org.framework.rodolfo.freire.git.asuna.packet.read;

import org.framework.rodolfo.freire.git.asuna.exception.ProtocolViolationException;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import java.nio.ByteBuffer;

public class CipherPacketReader implements PacketReader {

    private final Cipher mCipher;
    private ByteBuffer mInternalBuffer;
    private PacketReader mReader;

    public CipherPacketReader(Cipher cipher, PacketReader reader) {
        mCipher = cipher;
        mReader = reader;
    }

    public PacketReader getReader() {
        return mReader;
    }

    public void setReader(PacketReader reader) {
        mReader = reader;
    }

    public byte[] nextPacket(ByteBuffer byteBuffer) throws ProtocolViolationException {
        if (mInternalBuffer == null) {
            mInternalBuffer = ByteBuffer.allocate(mCipher.getOutputSize(byteBuffer.remaining()));
        } else {
            if (byteBuffer.remaining() > 0) {
                ByteBuffer newBuffer = ByteBuffer.allocate(mCipher.getOutputSize(byteBuffer.remaining()) + mInternalBuffer.remaining());
                newBuffer.put(mInternalBuffer);
                mInternalBuffer = newBuffer;
            }
        }
        if (byteBuffer.remaining() > 0) {
            try {
                mCipher.update(byteBuffer, mInternalBuffer);
            } catch (ShortBufferException e) {
                throw new ProtocolViolationException("Short buffer");
            }
            mInternalBuffer.flip();
        }
        byte[] packet = mReader.nextPacket(mInternalBuffer);
        if (mInternalBuffer.remaining() == 0) mInternalBuffer = null;
        return packet;
    }

}
