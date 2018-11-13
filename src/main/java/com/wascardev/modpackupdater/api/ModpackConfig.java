package com.wascardev.modpackupdater.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class ModpackConfig {

    private String name;
    private String downloadFolderURL;
    private List<File> minecraftFiles;
    private List<File> modpackFiles;
    private List<File> optionalFiles;

    public ModpackConfig(String name, String downloadFolderURL) {
        this.name = name;
        this.downloadFolderURL = downloadFolderURL;
    }

    public ModpackConfig(String name, String downloadFolderURL, List<File> minecraftFiles, List<File> modpackFiles, List<File> optionalFiles) {
        this.name = name;
        this.downloadFolderURL = downloadFolderURL;
        this.minecraftFiles = minecraftFiles;
        this.modpackFiles = modpackFiles;
        this.optionalFiles = optionalFiles;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<File> getMinecraftFiles() {
        return minecraftFiles;
    }

    public void setMinecraftFiles(List<File> minecraftFiles) {
        this.minecraftFiles = minecraftFiles;
    }

    public List<File> getModpackFiles() {
        return modpackFiles;
    }

    public void setModpackFiles(List<File> modpackFiles) {
        this.modpackFiles = modpackFiles;
    }

    public List<File> getOptionalFiles() {
        return optionalFiles;
    }

    public void setOptionalFiles(List<File> optionalFiles) {
        this.optionalFiles = optionalFiles;
    }

    public String getName() {
        return name;
    }

    public URL getDownloadFolderURL() throws MalformedURLException {
        return new URL(downloadFolderURL);
    }

    public static class File {

        protected String hash = null;
        protected String path = null;

        public File(String path, String hash) {
            this.hash = hash;
            this.path = path;
        }


        public String getHash() {
            return hash;
        }

        public String getPath() {
            return path;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof File)
                return ((File)obj).getPath().equals(this.path);
            return false;
        }
    }


}
