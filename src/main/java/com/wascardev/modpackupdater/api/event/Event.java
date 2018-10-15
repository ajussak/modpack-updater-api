package com.wascardev.modpackupdater.api.event;

import com.wascardev.modpackupdater.api.WorkState;

public class Event {

    private WorkState workState;

    public Event(WorkState workState) {
        this.workState = workState;
    }

    public WorkState getWorkState() {
        return workState;
    }
}
