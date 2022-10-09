package com.wascardev.modpackupdater.api;

import com.google.gson.*;
import com.wascardev.modpackupdater.api.event.DownloadEvent;
import com.wascardev.modpackupdater.api.event.ErrorEvent;
import com.wascardev.modpackupdater.api.event.Event;
import com.wascardev.modpackupdater.api.event.EventListener;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ModpackUpdater implements Runnable {

    private static final File LAUNCHER_PATH = Util.getClientDirectory("minecraft");
    private final static Logger LOGGER = Logger.getLogger("ModPack Updater");

    private File modpackPath;
    private File clientConfigPath;

    private ModpackConfig modpackConfig;
    private ClientConfig clientConfig;
    private Gson gson;

    private List<EventListener> eventListeners = new ArrayList<>();

    private int filesToDownload;
    private int downloadingFile = 0;

    public static ModpackUpdater createUpdaterFromJson(URL jsonURL) throws IOException {
        GsonBuilder gsonBuilder = new GsonBuilder();

        JsonDeserializer<ModpackConfig> deserializer = (json, typeOfT, context) -> {
            JsonObject jsonObject = json.getAsJsonObject();

            List<ModpackConfig.File> minecraftFiles = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : jsonObject.get("minecraftFiles").getAsJsonObject().entrySet()) {
                minecraftFiles.add(new ModpackConfig.File(entry.getKey(), entry.getValue().getAsString()));
            }

            List<ModpackConfig.File> modpackFiles = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : jsonObject.get("modpackFiles").getAsJsonObject().entrySet()) {
                modpackFiles.add(new ModpackConfig.File(entry.getKey(), entry.getValue().getAsString()));
            }

            List<ModpackConfig.File> optionalFiles = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : jsonObject.get("optionalFiles").getAsJsonObject().entrySet()) {
                optionalFiles.add(new ModpackConfig.File(entry.getKey(), entry.getValue().getAsString()));
            }

            return new ModpackConfig(jsonObject.get("name").getAsString(), jsonObject.get("downloadFolderURL").getAsString(), minecraftFiles, modpackFiles, optionalFiles);
        };

        gsonBuilder.registerTypeAdapter(ModpackConfig.class, deserializer);

        Gson customGson = gsonBuilder.create();

        return new ModpackUpdater(customGson.fromJson(new InputStreamReader(jsonURL.openStream()), ModpackConfig.class));
    }

    public ModpackUpdater(ModpackConfig modpackConfig) throws IOException {
        this.modpackConfig = modpackConfig;
        modpackPath = Util.getClientDirectory(modpackConfig.getName());
        clientConfigPath = new File(modpackPath, "updater_config.json");

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();

        gson = gsonBuilder.create();

        if (clientConfigPath.exists())
            clientConfig = gson.fromJson(new FileReader(clientConfigPath), ClientConfig.class);
        else {
            clientConfig = new ClientConfig();
            FileWriter fileWriter = new FileWriter(clientConfigPath);
            gson.toJson(clientConfig, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        }
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
                float currentProgress = (float) downloadedFileSize / (float) completeFileSize;
                sendEvent(new DownloadEvent(currentProgress, ((float) this.downloadingFile / (float) this.filesToDownload) + currentProgress * (1.0f / this.filesToDownload)));
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
        sendEvent(new Event(WorkState.CHECKING));

        List<ModpackConfig.File> selectedOptionalFiles = modpackConfig.getOptionalFiles().stream().filter(e -> clientConfig.getSelectedOptionalMods().contains(e.path)).collect(Collectors.toList());


        List<ModpackConfig.File> authorizedFile = new ArrayList<>();

        authorizedFile.addAll(modpackConfig.getModpackFiles());
        authorizedFile.addAll(selectedOptionalFiles);

        File modsFolder = new File(modpackPath, "mods");
        if (modsFolder.exists())
            deleteUnknownFiles(authorizedFile, modpackPath, modsFolder);

        authorizedFile.addAll(modpackConfig.getMinecraftFiles());

        filesToDownload = authorizedFile.size();

        downloadFileGroup(modpackConfig.getModpackFiles(), new URL(modpackConfig.getDownloadFolderURL(), "modpack"), modpackPath);
        downloadFileGroup(selectedOptionalFiles, new URL(modpackConfig.getDownloadFolderURL(), "optionals"), modpackPath);
        downloadFileGroup(modpackConfig.getMinecraftFiles(), new URL(modpackConfig.getDownloadFolderURL(), "minecraft"), LAUNCHER_PATH);
        sendEvent(new DownloadEvent(1f, 1f));
    }

    private void deleteUnknownFiles(List<ModpackConfig.File> files, File directory, File base) throws IOException {
        for (File currentFile : Objects.requireNonNull(base.listFiles())) {
            if (currentFile.isDirectory())
                deleteUnknownFiles(files, directory, currentFile);
            else if (!files.contains(new ModpackConfig.File(directory.toURI().relativize(currentFile.toURI()).getPath(), ""))) {
                LOGGER.log(Level.INFO, "Unknown file deleted : " + currentFile.getPath());
                if (!currentFile.delete())
                    throw new IOException("Cannot delete : " + currentFile.getAbsolutePath());
            }
        }
    }

    private void downloadFileGroup(List<ModpackConfig.File> fileGroup, URL downloadFolderURL, File destinationFolder) throws Exception {
        for (ModpackConfig.File modpackFile : fileGroup) {
            File currentFile = new File(destinationFolder, modpackFile.getPath());
            if (clientConfig.getForceUpdate() || !currentFile.exists() || !Util.verifyChecksum(currentFile, modpackFile.getHash())) {
                downloadFromUrl(new URL(appendSegmentToPath(downloadFolderURL.toString(), modpackFile.getPath())), currentFile);
            }
            this.downloadingFile++;
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
        sendEvent(new Event(WorkState.CREATING_PROFILE));
        File file = new File(LAUNCHER_PATH, "launcher_profiles.json");
        Gson gson = new Gson();
        JsonObject jsonObject = (file.exists()) ? gson.fromJson(new InputStreamReader(new FileInputStream(file)), JsonObject.class) : new JsonObject();

        if (jsonObject == null)
            jsonObject = new JsonObject();

        JsonObject profiles = jsonObject.getAsJsonObject("profiles");
        if (profiles == null)
            profiles = new JsonObject();

        if (profiles.get(modpackConfig.getName()) == null || clientConfig.getForceUpdate()) {
            JsonObject modpackProfile = new JsonObject();
            profiles.remove(modpackConfig.getName());
            modpackProfile.addProperty("name", modpackConfig.getName());
            assert modpackPath != null;
            modpackProfile.addProperty("gameDir", modpackPath.getPath());
            modpackProfile.addProperty("lastVersionId", modpackConfig.getName());
            modpackProfile.addProperty("javaArgs", "-Xmx" + clientConfig.getRam() + "M -Xmn128M");
            profiles.add(modpackConfig.getName(), modpackProfile);
            jsonObject.remove("profiles");
            jsonObject.add("profiles", profiles);
        }
        Files.write(file.toPath(), gson.toJson(jsonObject).getBytes());
        LOGGER.log(Level.INFO, "Installed");

    }

    public void startGame() throws IOException, MinecraftLauncerNotFoundException {
        sendEvent(new Event(WorkState.LAUNCHING));
        String path = clientConfig.getMinecraftLauncherPath();

        if (path.equals(""))
            throw new MinecraftLauncerNotFoundException();

        ProcessBuilder processBuilder = path.endsWith(".jar") ? new ProcessBuilder("java", "-jar", path) : new ProcessBuilder(path);
        processBuilder.start();
    }

    public void setClientConfig(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    public void run() {
        try {
            download();
            createProfile();
            startGame();
            sendEvent(new Event(WorkState.FINISHED));
        } catch (MinecraftLauncerNotFoundException e) {
            sendEvent(new Event(WorkState.MINECRAFT_LAUNCHER_NOT_FOUND));
        } catch (Exception e) {
            sendEvent(new ErrorEvent(e));
        }
    }

    private void sendEvent(Event event) {
        eventListeners.forEach(eventListener -> eventListener.onEvent(event));
    }

    public List<ModpackConfig.File> getOptionalFiles() {
        return modpackConfig.getOptionalFiles();
    }

    public void addEventListener(EventListener eventListener) {
        eventListeners.add(eventListener);
    }

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public void saveClientConfig() throws IOException {
        FileWriter fileWriter = new FileWriter(clientConfigPath);
        gson.toJson(clientConfig, fileWriter);
        fileWriter.flush();
        fileWriter.close();
    }
}
