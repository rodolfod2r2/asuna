package org.framework.rodolfo.freire.git.asuna.socket.channel;

import org.framework.rodolfo.freire.git.asuna.socket.NIOAbstractSocket;
import org.framework.rodolfo.freire.git.asuna.socket.NIOService;
import org.framework.rodolfo.freire.git.asuna.util.NIOUtils;

import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public abstract class ChannelResponder implements NIOAbstractSocket {

    private final NIOService nioService;
    private final String mIp;
    private final InetSocketAddress inetSocketAddress;
    private final int mPort;
    private final SelectableChannel selectableChannel;
    private volatile boolean mOpen;
    private volatile SelectionKey selectionKey;
    private volatile int mInterestOps;
    private boolean mObserverSet;
    private Object mTag;

    public ChannelResponder(NIOService service, SelectableChannel channel, InetSocketAddress address) {
        selectableChannel = channel;
        nioService = service;
        mOpen = true;
        selectionKey = null;
        mInterestOps = 0;
        mObserverSet = false;
        inetSocketAddress = address;
        mIp = address.getAddress().getHostAddress();
        mPort = address.getPort();
        mTag = null;
    }


    public InetSocketAddress getAddress() {
        return inetSocketAddress;
    }

    public String getIp() {
        return mIp;
    }

    public int getPort() {
        return mPort;
    }

    public Object getTag() {
        return mTag;
    }

    public void setTag(Object tag) {
        mTag = tag;
    }

    public NIOService getNIOService() {
        return nioService;
    }

    public void markObserverSet() {
        synchronized (this) {
            if (mObserverSet) throw new IllegalStateException("Listener already set.");
            mObserverSet = true;
        }
    }

    public void socketReadyForRead() {
        throw new UnsupportedOperationException(getClass() + " does not support read.");
    }

    public void socketReadyForWrite() {
        throw new UnsupportedOperationException(getClass() + " does not support write.");
    }

    public void socketReadyForAccept() {
        throw new UnsupportedOperationException(getClass() + " does not support accept.");
    }

    public void socketReadyForConnect() {
        throw new UnsupportedOperationException(getClass() + " does not support connect.");
    }

    public SelectableChannel getChannel() {
        return selectableChannel;
    }

    public SelectionKey getKey() {
        return selectionKey;
    }
    public void setKey(SelectionKey key) {
        if (selectionKey != null) throw new IllegalStateException("Tried to set selection key twice");
        selectionKey = key;
        if (!isOpen()) {
            NIOUtils.cancelKeySilently(selectionKey);
            return;
        }
        keyInitialized();
        synchronizeKeyInterestOps();
    }

    public abstract void keyInitialized();

    public boolean isOpen() {
        return mOpen;
    }

    public void close() {
        close(null);
    }

    public void close(Exception exception) {
        if (isOpen()) {
            getNIOService().queue(new CloseEvent(this, exception));
        }
    }

    private void synchronizeKeyInterestOps() {
        if (selectionKey != null) {
            try {
                int oldOps = selectionKey.interestOps();
                if ((mInterestOps & SelectionKey.OP_CONNECT) != 0) {
                    selectionKey.interestOps(SelectionKey.OP_CONNECT);
                } else {
                    selectionKey.interestOps(mInterestOps);
                }
                if (selectionKey.interestOps() != oldOps) {
                    nioService.wakeup();
                }
            } catch (CancelledKeyException e) {
                // Ignore these.
            }
        }
    }

    public void deleteInterest(int interest) {
        mInterestOps = mInterestOps & ~interest;
        synchronizeKeyInterestOps();
    }

    public void addInterest(int interest) {
        mInterestOps |= interest;
        synchronizeKeyInterestOps();
    }

    @Override
    public String toString() {
        return mIp + ":" + mPort;
    }

    public abstract void shutdown(Exception e);

    private static class CloseEvent implements Runnable {
        private final ChannelResponder m_responder;
        private final Exception m_exception;

        private CloseEvent(ChannelResponder responder, Exception e) {
            m_responder = responder;
            m_exception = e;
        }

        public void run() {
            if (m_responder.isOpen()) {
                m_responder.mOpen = false;
                NIOUtils.closeKeyAndChannelSilently(m_responder.getKey(), m_responder.getChannel());
                m_responder.shutdown(m_exception);
            }
        }
    }

}
