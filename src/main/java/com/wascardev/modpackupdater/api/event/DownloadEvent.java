package com.wascardev.modpackupdater.api.event;

import com.wascardev.modpackupdater.api.WorkState;

public class DownloadEvent extends Event {

    private float progressFile;
    private float progressGlobal;

    public DownloadEvent(float progressFile, float progressGlobal) {
        super(WorkState.DOWNLOADING);
        this.progressFile = progressFile;
        this.progressGlobal = progressGlobal;
    }

    public float getProgressFile() {
        return progressFile;
    }

    public float getProgressGlobal() {
        return progressGlobal;
    }
}
