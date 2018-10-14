package com.wascardev.modpackupdater.api;

public interface EventListener {

    void onStateChanged(WorkState newState);
    void onDownloadStateChanged(String fileName, int percentage);
    void onError(Exception exception);

}
