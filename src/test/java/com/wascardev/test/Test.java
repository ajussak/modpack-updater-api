package com.wascardev.test;

import com.wascardev.modpackupdater.api.ModpackUpdater;

import java.io.IOException;
import java.net.URL;

public class Test {

    public static void main(String[] args)
    {
        try {
            ModpackUpdater modpackUpdater = new ModpackUpdater(new URL(args[0]));
            modpackUpdater.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
