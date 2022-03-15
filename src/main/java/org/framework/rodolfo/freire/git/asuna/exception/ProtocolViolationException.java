package org.framework.rodolfo.freire.git.asuna.exception;

import java.io.IOException;

public class ProtocolViolationException extends IOException {
    private static final long serialVersionUID = 6869467292395980590L;

    public ProtocolViolationException(String message) {
        super(message);
    }

}
