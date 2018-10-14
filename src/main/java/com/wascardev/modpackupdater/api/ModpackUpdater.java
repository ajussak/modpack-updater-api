package com.wascardev.modpackupdater.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ModpackUpdater implements Runnable {

    private static final File LAUNCHER_PATH = Util.getClientDirectory("minecraft");
    private final static Logger LOGGER = Logger.getLogger("ModPack Updater");

    private File modpackPath;

    private WorkState currentState;
    private EventListener eventListener = null;
    private ModpackConfig modpackConfig;
    private ClientConfig clientConfig = new ClientConfig();

    public ModpackUpdater(URL jsonURL) throws IOException {
        this(new Gson().fromJson(new InputStreamReader(jsonURL.openStream()), ModpackConfig.class));
    }

    public ModpackUpdater(ModpackConfig modpackConfig)
    {
        this.modpackConfig = modpackConfig;
        modpackPath = Util.getClientDirectory(modpackConfig.getName());
        currentState = WorkState.READY;
    }

    private void downloadFromUrl(URL url, File localFile) throws Exception {
        LOGGER.log(Level.INFO, "Downloading from : " + url.getPath());
        InputStream is = null;
        FileOutputStream fos = null;
        File parent = localFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs())
            throw new Exception("Folder could not be created : " + parent.getPath());

        try {
            HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
            long completeFileSize = httpConnection.getContentLength();

            is = httpConnection.getInputStream();
            fos = new FileOutputStream(localFile);

            byte[] buffer = new byte[4096];
            int len;
            long downloadedFileSize = 0;

            while ((len = is.read(buffer)) > 0) {
                downloadedFileSize += len;
                int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 100d);
                if(eventListener != null)
                    eventListener.onDownloadStateChanged(localFile.getName(), currentProgress);
                fos.write(buffer, 0, len);
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        }
    }

    private void download() throws Exception {
        changeState(WorkState.DOWNLOADING);

        List<ModpackConfig.File> selectedOptionalFiles = modpackConfig.getOptionalFiles().getFiles().stream().filter(e -> clientConfig.getSelectedOptionalMods().contains(e.path)).collect(Collectors.toList());

        ModpackConfig.FileGroup optionalFileGroup = new ModpackConfig.FileGroup(modpackConfig.getOptionalFiles().getUrl(), selectedOptionalFiles);

        List<ModpackConfig.File> authorizedFile = new ArrayList<>();

        authorizedFile.addAll(modpackConfig.getModpackFiles().getFiles());
        authorizedFile.addAll(optionalFileGroup.getFiles());

        File modsFolder = new File(modpackPath, "mods");

        if(modsFolder.exists())
            deleteUnknownFiles(authorizedFile, modpackPath, modsFolder);

        downloadFileGroup(modpackConfig.getModpackFiles(), modpackPath);
        downloadFileGroup(optionalFileGroup, modpackPath);
        downloadFileGroup(modpackConfig.getMinecraftFiles(), LAUNCHER_PATH);
    }

    private void deleteUnknownFiles(List<ModpackConfig.File> files, File directory, File base)
    {
        for(File currentFile : Objects.requireNonNull(base.listFiles()))
        {
            if(currentFile.isDirectory())
                deleteUnknownFiles(files, directory, currentFile);
            else if(!files.contains(new ModpackConfig.File(directory.toURI().relativize(currentFile.toURI()).getPath(), ""))) {
                LOGGER.log(Level.INFO, "Unknown file deleted : " + currentFile.getPath());
                currentFile.delete();
            }
        }
    }

    private void downloadFileGroup(ModpackConfig.FileGroup fileGroup, File destinationFolder) throws Exception {
        for(ModpackConfig.File modpackFile : fileGroup.getFiles())
        {
            File currentFile = new File(destinationFolder, modpackFile.getPath());
            if(!currentFile.exists() || !Util.verifyChecksum(currentFile, modpackFile.getHash()))
            {
                downloadFromUrl(new URL(appendSegmentToPath(fileGroup.getUrl(), modpackFile.getPath())), currentFile);
            }
        }
    }

    private String appendSegmentToPath(String path, String segment) {
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        if (path.charAt(path.length() - 1) == '/' || segment.startsWith("/")) {
            return path + segment;
        }

        return path + "/" + segment;
    }

    private void createProfile() throws IOException {
        LOGGER.log(Level.INFO, "Creating profile...");
        changeState(WorkState.CREATING_PROFILE);
        File file = new File(LAUNCHER_PATH, "launcher_profiles.json");
        Gson gson = new Gson();
        JsonObject jsonObject = (file.exists()) ? gson.fromJson(new InputStreamReader(new FileInputStream(file)), JsonObject.class) : new JsonObject();

        if (jsonObject == null)
            jsonObject = new JsonObject();

        JsonObject profiles = jsonObject.getAsJsonObject("profiles");
        if (profiles == null)
            profiles = new JsonObject();

        if(profiles.get(modpackConfig.getName()) == null || clientConfig.getForceUpdate()) {
            JsonObject modpackProfile = new JsonObject();
            profiles.remove(modpackConfig.getName());
            modpackProfile.addProperty("name", modpackConfig.getName());
            assert modpackPath != null;
            modpackProfile.addProperty("gameDir", modpackPath.getPath());
            modpackProfile.addProperty("lastVersionId", modpackConfig.getName());
            modpackProfile.addProperty("javaArgs", "-Xmx" + clientConfig.getRam() + "M -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:-UseAdaptiveSizePolicy -Xmn128M");
            profiles.add(modpackConfig.getName(), modpackProfile);
            jsonObject.remove("profiles");
            jsonObject.add("profiles", profiles);
        }
        jsonObject.remove("selectedProfile");
        jsonObject.addProperty("selectedProfile", modpackConfig.getName());
        Files.write(file.toPath(), gson.toJson(jsonObject).getBytes());
        LOGGER.log(Level.INFO, "Done");
    }

    @Override
    public void run() {
        try {
            download();
            createProfile();
        } catch (Exception e) {
            e.printStackTrace();
            changeState(WorkState.ERROR);
            if(eventListener != null)
                eventListener.onError(e);
        }
    }

    private void changeState(WorkState workState)
    {
        currentState = workState;
        if(eventListener != null)
            eventListener.onStateChanged(currentState);
    }

    public WorkState getCurrentState() {
        return currentState;
    }

    public ModpackConfig.FileGroup getOptionalFiles()
    {
        return modpackConfig.getOptionalFiles();
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }
}
