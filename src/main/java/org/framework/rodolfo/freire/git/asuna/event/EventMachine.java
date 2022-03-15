package org.framework.rodolfo.freire.git.asuna.event;

import org.framework.rodolfo.freire.git.asuna.exception.ExceptionObserver;
import org.framework.rodolfo.freire.git.asuna.socket.NIOService;

import java.io.IOException;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;


public class EventMachine {

    private final NIOService nioService;
    private final Queue<DelayedAction> delayedActionQueue;
    private Thread runnerThread;

    public EventMachine() throws IOException {
        nioService = new NIOService();
        delayedActionQueue = new PriorityBlockingQueue<>();
        runnerThread = null;
    }

    public void asyncExecute(Runnable runnable) {
        executeLater(runnable, 0);
    }

    public DelayedEvent executeLater(Runnable runnable, long msDelay) {
        return queueAction(runnable, msDelay + System.currentTimeMillis());
    }


    private DelayedAction queueAction(Runnable runnable, long time) {
        DelayedAction action = new DelayedAction(runnable, time);
        delayedActionQueue.add(action);
        nioService.wakeup();
        return action;
    }

    public DelayedEvent executeAt(Runnable runnable, Date date) {
        return queueAction(runnable, date.getTime());
    }


    public void setObserver(ExceptionObserver observer) {
        getNIOService().setExceptionObserver(observer);
    }

    public long timeOfNextEvent() {
        DelayedAction action = delayedActionQueue.peek();
        return action == null ? Long.MAX_VALUE : action.getTime();
    }


    public synchronized void start() {
        if (runnerThread != null) throw new IllegalStateException("Service already running.");
        if (!nioService.isOpen()) throw new IllegalStateException("Service has been shut down.");
        runnerThread = new Thread() {
            @Override
            public void run() {
                while (runnerThread == this) {
                    try {
                        select();
                    } catch (Throwable e) {
                        if (runnerThread == this) getNIOService().notifyException(e);
                    }
                }
            }
        };
        runnerThread.start();
    }

    public synchronized void stop() {
        if (runnerThread == null) throw new IllegalStateException("Service is not running.");
        runnerThread = null;
        nioService.wakeup();
    }

    public synchronized void shutdown() {
        if (runnerThread == null) throw new IllegalStateException("The service is not running.");
        nioService.close();
        stop();
    }

    private void select() throws Throwable {
        // Run queued actions to be called
        while (timeOfNextEvent() <= System.currentTimeMillis()) {
            try {
                runNextAction();
            } catch (Throwable t) {
                getNIOService().notifyException(t);
            }
        }
        if (timeOfNextEvent() == Long.MAX_VALUE) {
            nioService.selectBlocking();
        } else {
            long delay = timeOfNextEvent() - System.currentTimeMillis();
            nioService.selectBlocking(Math.max(1, delay));
        }
    }

    private void runNextAction() {
        delayedActionQueue.poll().run();
    }

    public NIOService getNIOService() {
        return nioService;
    }

    public Queue<DelayedEvent> getQueue() {
        return new PriorityQueue<>(delayedActionQueue);
    }

    public int getQueueSize() {
        return delayedActionQueue.size();
    }

}
