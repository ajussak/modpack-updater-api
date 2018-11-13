package com.wascardev.modpackupdater.modpackmanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.security.NoSuchAlgorithmException;

public class ModpackManager extends Application {

    private FileListTab minecraftFilesTab;
    private FileListTab modpackFilesTab;
    private FileListTab optionalFilesTab;
    private TextField modpackNameTextfield;
    private File currentProject;
    private File minecraftFolder;
    private File modpackFolder;
    private File optionalFolder;

    public static void main(String[] args) {
        Application.launch(ModpackManager.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        primaryStage.setTitle("ModPack Manager - Empty Project");
        Group root = new Group();
        Scene scene = new Scene(root, 500, 400);

        VBox vBox = new VBox();

        MenuBar menuBar = new MenuBar();
        menuBar.prefWidthProperty().bind(primaryStage.widthProperty());

        Menu menuFile = new Menu("File");

        MenuItem newProjectItem = new MenuItem("New Project...");

        MenuItem openProjectItem = new MenuItem("Open Project...");

        MenuItem saveProjectItem = new MenuItem("Save Project");
        saveProjectItem.setOnAction(event -> {

        });

        MenuItem exportProject = new MenuItem("Export JSON");

        MenuItem quitItem = new MenuItem("Quit");
        quitItem.setOnAction(event -> Platform.exit());

        menuFile.getItems().addAll(newProjectItem, openProjectItem, saveProjectItem, new SeparatorMenuItem(), exportProject, new SeparatorMenuItem(), quitItem);

        menuBar.getMenus().add(menuFile);

        HBox hBox = new HBox();

        modpackNameTextfield = new TextField();

        hBox.getChildren().addAll(new Label("Modpack Name : "), modpackNameTextfield);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        minecraftFilesTab = new FileListTab("Minecraft Files", minecraftFolder);
        modpackFilesTab = new FileListTab("Modpack Files", modpackFolder);
        optionalFilesTab = new FileListTab("Modpack Optionals Files", optionalFolder);

        tabPane.getTabs().addAll(minecraftFilesTab, modpackFilesTab, optionalFilesTab);

        vBox.getChildren().addAll(menuBar, hBox, tabPane);

        root.getChildren().add(vBox);

        newProjectItem.setOnAction(event -> {
            resetProject();
            primaryStage.setTitle("ModPack Manager - Empty Project");
        });

        openProjectItem.setOnAction(event -> {
            resetProject();

            selectProject(primaryStage, false);

            Gson gson = new Gson();
            try {
                JsonObject jsonObject = gson.fromJson(new InputStreamReader(new FileInputStream(this.currentProject)), JsonObject.class);

                modpackNameTextfield.setText(jsonObject.getAsJsonPrimitive("name").getAsString());

                minecraftFilesTab.loadTabFromJSON(jsonObject.getAsJsonArray("minecraftFiles"));
                modpackFilesTab.loadTabFromJSON(jsonObject.getAsJsonArray("modpackFiles"));
                optionalFilesTab.loadTabFromJSON(jsonObject.getAsJsonArray("optionalFiles"));

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });

        saveProjectItem.setOnAction(event -> {

            if(this.currentProject == null) {
                selectProject(primaryStage, true);
            }

            try {
                saveJson(this.currentProject);
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        });

        exportProject.setOnAction(event -> {

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export JSON");

            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json");
            fileChooser.setInitialFileName("manifest.json");
            fileChooser.getExtensionFilters().add(extFilter);

            File file = fileChooser.showSaveDialog(primaryStage);

            try {
                saveJson(file);
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

        });

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void saveJson(File file) throws IOException, NoSuchAlgorithmException {
        if (file != null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("name", new JsonPrimitive(modpackNameTextfield.getText().replace(" ", "-")));

            jsonObject.add("minecraftFiles", minecraftFilesTab.generateJsonArray());
            jsonObject.add("modpackFiles", modpackFilesTab.generateJsonArray());
            jsonObject.add("optionalFiles", optionalFilesTab.generateJsonArray());

            Writer writer = new FileWriter(file);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonObject, writer);
            writer.flush();
            writer.close();
        }
    }

    private void selectProject(Stage stage, boolean save)
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Project");

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Modpack Project (*.modpack)", "*.modpack");
        fileChooser.setInitialFileName(modpackNameTextfield.getText().replace(" ", "-") + ".json");
        fileChooser.getExtensionFilters().add(extFilter);

        this.currentProject = save ? fileChooser.showSaveDialog(stage) : fileChooser.showOpenDialog(stage);

        this.minecraftFolder = new File(currentProject, "minecraft");
        this.optionalFolder = new File(currentProject, "optional");
        this.modpackFolder = new File(currentProject, "modpack");

        stage.setTitle("ModPack Manager - " + this.currentProject.getName());
    }

    private void resetProject()
    {
        currentProject = null;
        modpackNameTextfield.setText("");
    }
}
