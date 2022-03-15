package org.framework.rodolfo.freire.git.asuna.event;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class DelayedAction implements Comparable<DelayedAction>, DelayedEvent {
    private final static AtomicLong nextId = new AtomicLong(0L);
    private final long time;
    private final long mId;
    private volatile Runnable mCall;

    public DelayedAction(Runnable call, long time) {
        mCall = call;
        this.time = time;
        mId = nextId.getAndIncrement();
    }

    public void cancel() {
        mCall = null;
    }

    void run() {
        Runnable call = mCall;
        if (call != null) call.run();
    }

    @SuppressWarnings({"AccessingNonPublicFieldOfAnotherObject"})
    public int compareTo(DelayedAction o) {
        if (time < o.time) return -1;
        if (time > o.time) return 1;
        if (mId < o.mId) return -1;
        return mId > o.mId ? 1 : 0;
    }

    public Runnable getCall() {
        return mCall;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "DelayedAction @ " + new Date(time) + " [" + (mCall == null ? "Cancelled" : mCall) + "]";
    }
}
