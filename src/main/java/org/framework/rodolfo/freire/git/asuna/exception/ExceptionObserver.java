package org.framework.rodolfo.freire.git.asuna.exception;

public interface ExceptionObserver {

    ExceptionObserver DEFAULT = Throwable::printStackTrace;

    void notifyExceptionThrown(Throwable e);
}
