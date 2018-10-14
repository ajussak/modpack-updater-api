package com.wascardev.modpackupdater.api;

import java.util.List;

public class ModpackConfig {

    private String name;
    private FileGroup minecraftFiles;
    private FileGroup modpackFiles;
    private FileGroup optionalFiles;

    public ModpackConfig(String name) {
        this.name = name;
    }

    public ModpackConfig(String name, FileGroup minecraftFiles, FileGroup modpackFiles, FileGroup optionalFiles) {
        this.name = name;
        this.minecraftFiles = minecraftFiles;
        this.modpackFiles = modpackFiles;
        this.optionalFiles = optionalFiles;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMinecraftFiles(FileGroup minecraftFiles) {
        this.minecraftFiles = minecraftFiles;
    }

    public void setModpackFiles(FileGroup modpackFiles) {
        this.modpackFiles = modpackFiles;
    }

    public void setOptionalFiles(FileGroup optionalFiles) {
        this.optionalFiles = optionalFiles;
    }

    public FileGroup getMinecraftFiles() {
        return minecraftFiles;
    }

    public FileGroup getModpackFiles() {
        return modpackFiles;
    }

    public FileGroup getOptionalFiles() {
        return optionalFiles;
    }

    public String getName() {
        return name;
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

    public static class FileGroup {
        private String url;
        private List<File> files = null;

        public FileGroup(String url, List<File> files) {
            this.url = url;
            this.files = files;
        }

        public String getUrl() {
            return url;
        }

        public List<File> getFiles() {
            return files;
        }
    }


}
