package org.framework.rodolfo.freire.git.asuna.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;

public class NIOUtils {

    NIOUtils() {
    }

    public static void closeKeyAndChannelSilently(SelectionKey key, Channel channel) {
        closeChannelSilently(channel);
        cancelKeySilently(key);
    }

    public static void setPacketSizeInByteBuffer(ByteBuffer byteBuffer, int headerSize, int valueToEncode, boolean bigEndian) {
        if (valueToEncode < 0) throw new IllegalArgumentException("Payload size is less than 0.");
        if (headerSize != 4 && valueToEncode >> (headerSize * 8) > 0) {
            throw new IllegalArgumentException("Payload size cannot be encoded into " + headerSize + " byte(s).");
        }
        for (int i = 0; i < headerSize; i++) {
            int index = bigEndian ? (headerSize - 1 - i) : i;
            byteBuffer.put((byte) (valueToEncode >> (8 * index) & 0xFF));
        }
    }

    public static void setHeaderForPacketSize(byte[] buffer, int headerSize, int valueToEncode, boolean bigEndian) {
        if (valueToEncode < 0) throw new IllegalArgumentException("Payload size is less than 0.");
        if (headerSize != 4 && valueToEncode >> (headerSize * 8) > 0) {
            throw new IllegalArgumentException("Payload size cannot be encoded into " + headerSize + " byte(s).");
        }
        for (int i = 0; i < headerSize; i++) {
            int index = bigEndian ? (headerSize - 1 - i) : i;
            buffer[i] = ((byte) (valueToEncode >> (8 * index) & 0xFF));
        }
    }

    public static int getPacketSizeFromByteBuffer(ByteBuffer header, int size, boolean bigEndian) {
        long packetSize = 0;
        if (bigEndian) {
            for (int i = 0; i < size; i++) {
                packetSize <<= 8;
                packetSize += header.get() & 0xFF;
            }
        } else {
            int shift = 0;
            for (int i = 0; i < size; i++) {
                packetSize += (header.get() & 0xFF) << shift;
                shift += 8;
            }
        }
        return (int) packetSize;
    }

    public static int getPacketSizeFromByteArray(byte[] data, int length, boolean bigEndian) {
        long packetSize = 0;
        if (bigEndian) {
            for (int i = 0; i < length; i++) {
                packetSize <<= 8;
                packetSize += data[i] & 0xFF;
            }
        } else {
            int shift = 0;
            for (int i = 0; i < length; i++) {
                // We do not need to extend valueToEncode here, since the maximum is valueToEncode >> 24
                packetSize += (data[i] & 0xFF) << shift;
                shift += 8;
            }
        }
        return (int) packetSize;
    }

    public static void closeChannelSilently(Channel channel) {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException e) {
            // Do nothing
        }
    }

    public static void cancelKeySilently(SelectionKey key) {
        try {
            if (key != null) key.cancel();
        } catch (Exception e) {
            // Do nothing
        }
    }

    public static ByteBuffer[] compact(ByteBuffer[] buffers) {
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i].remaining() > 0) {
                if (i == 0) return buffers;
                ByteBuffer[] newBuffers = new ByteBuffer[buffers.length - i];
                System.arraycopy(buffers, i, newBuffers, 0, buffers.length - i);
                return newBuffers;
            }
        }
        return null;
    }

    public static ByteBuffer[] concat(ByteBuffer[] buffers, ByteBuffer buffer) {
        return concat(buffers, new ByteBuffer[]{buffer});
    }

    public static ByteBuffer[] concat(ByteBuffer buffer, ByteBuffer[] buffers2) {
        return concat(new ByteBuffer[]{buffer}, buffers2);
    }

    public static ByteBuffer[] concat(ByteBuffer[] buffers1, ByteBuffer[] buffers2) {
        if (buffers1 == null || buffers1.length == 0) return buffers2;
        if (buffers2 == null || buffers2.length == 0) return buffers1;
        ByteBuffer[] newBuffers = new ByteBuffer[buffers1.length + buffers2.length];
        System.arraycopy(buffers1, 0, newBuffers, 0, buffers1.length);
        System.arraycopy(buffers2, 0, newBuffers, buffers1.length, buffers2.length);
        return newBuffers;
    }

    public static ByteBuffer copy(ByteBuffer buffer) {
        if (buffer == null) return null;
        ByteBuffer copy = ByteBuffer.allocate(buffer.remaining());
        copy.put(buffer);
        copy.flip();
        return copy;
    }

    public static long remaining(ByteBuffer[] byteBuffers) {
        long length = 0;
        for (ByteBuffer buffer : byteBuffers) length += buffer.remaining();
        return length;
    }

    public static boolean isEmpty(ByteBuffer[] byteBuffers) {
        for (ByteBuffer buffer : byteBuffers) {
            if (buffer.remaining() > 0) return false;
        }
        return true;
    }

    public static ByteBuffer join(ByteBuffer buffer1, ByteBuffer buffer2) {
        if (buffer2 == null || buffer2.remaining() == 0) return NIOUtils.copy(buffer1);
        if (buffer1 == null || buffer1.remaining() == 0) return NIOUtils.copy(buffer2);
        ByteBuffer buffer = ByteBuffer.allocate(buffer1.remaining() + buffer2.remaining());
        buffer.put(buffer1);
        buffer.put(buffer2);
        buffer.flip();
        return buffer;
    }
}