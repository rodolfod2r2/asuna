package org.framework.rodolfo.freire.git.asuna.packet.write;

import javax.crypto.Cipher;
import java.nio.ByteBuffer;

public class CipherPacketWriter implements PacketWriter {

    private final Cipher mCipher;
    private PacketWriter mPacketWriter;

    public CipherPacketWriter(Cipher cipher, PacketWriter packetWriter) {
        mCipher = cipher;
        mPacketWriter = packetWriter;
    }

    public PacketWriter getPacketWriter() {
        return mPacketWriter;
    }

    public void setPacketWriter(PacketWriter packetWriter) {
        mPacketWriter = packetWriter;
    }

    public ByteBuffer[] write(ByteBuffer[] byteBuffer) {
        byteBuffer = mPacketWriter.write(byteBuffer);
        ByteBuffer[] resultBuffer = new ByteBuffer[byteBuffer.length];
        try {
            for (int i = 0; i < byteBuffer.length; i++) {
                resultBuffer[i] = ByteBuffer.allocate(mCipher.getOutputSize(byteBuffer[i].remaining()));
                if (i == byteBuffer.length - 1) {
                    mCipher.doFinal(byteBuffer[i], resultBuffer[i]);
                } else {
                    mCipher.update(byteBuffer[i], resultBuffer[i]);
                }
                assert byteBuffer[i].remaining() == 0;
                resultBuffer[i].flip();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resultBuffer;
    }
}
