package com.wascardev.modpackupdater.api.event;

import com.wascardev.modpackupdater.api.WorkState;

public class ErrorEvent extends Event {

    private Exception exception;

    public ErrorEvent(Exception exception) {
        super(WorkState.ERROR);
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
