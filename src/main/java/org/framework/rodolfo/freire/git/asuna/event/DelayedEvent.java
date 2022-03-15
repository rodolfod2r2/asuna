package org.framework.rodolfo.freire.git.asuna.event;

public interface DelayedEvent {
    void cancel();

    Runnable getCall();

    long getTime();
}
