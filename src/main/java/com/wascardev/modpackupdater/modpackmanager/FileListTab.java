package com.wascardev.modpackupdater.modpackmanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.wascardev.modpackupdater.api.Util;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FileListTab extends Tab {

    private File workingDirectory;
    private TableView<FileValue> table;
    private TextField urlField;
    private TextField workingDirectoryPathField;

    public FileListTab(String text) {
        super(text);

        VBox vBox = new VBox();
        vBox.setFillWidth(true);

        HBox hBox1 = new HBox();

        this.workingDirectoryPathField = new TextField();
        HBox.setHgrow(workingDirectoryPathField, Priority.ALWAYS);

        Button selectFile = new Button("Open Folder");

        selectFile.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File folder = chooser.showDialog(vBox.getScene().getWindow());
            if (folder != null) {
                workingDirectoryPathField.setText(folder.getAbsolutePath());
            }
        });

        Button reload = new Button("Generate");

        hBox1.getChildren().addAll(new Label("Working Directory : "), workingDirectoryPathField, selectFile, reload);

        HBox hBox2 = new HBox();

        this.urlField = new TextField();

        HBox.setHgrow(urlField, Priority.ALWAYS);

        hBox2.getChildren().addAll(new Label("Download Folder URL : "), urlField);

        this.table = new TableView<>();

        TableColumn filePathCol = new TableColumn("File Path");
        filePathCol.prefWidthProperty().bind(table.widthProperty().multiply(0.7));
        filePathCol.setResizable(false);
        filePathCol.setCellValueFactory(new PropertyValueFactory<FileValue, String>("path"));

        TableColumn checkIntegrityCol = new TableColumn("Check Integrity");
        checkIntegrityCol.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
        checkIntegrityCol.setResizable(false);
        checkIntegrityCol.setCellValueFactory(new PropertyValueFactory<FileValue, String>("integrityCheckBox"));

        table.getColumns().addAll(filePathCol, checkIntegrityCol);

        table.setItems(FXCollections.observableArrayList());

        reload.setOnAction(e -> {
            workingDirectory = new File(workingDirectoryPathField.getText());

            List<FileValue> localFilesValues = scanFolder(workingDirectory);

            table.getItems().removeIf(fileValue -> !localFilesValues.contains(fileValue));
            table.getItems().addAll(localFilesValues.stream().filter(fileValue -> !table.getItems().contains(fileValue)).collect(Collectors.toList()));
        });

        VBox.setVgrow(table, Priority.ALWAYS);

        vBox.getChildren().addAll(hBox1, hBox2, table);

        this.setContent(vBox);
    }

    private List<FileValue> scanFolder(File folder) {
        ArrayList<FileValue> fileValueList = new ArrayList<>();
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isDirectory())
                fileValueList.addAll(scanFolder(file));
            else
                fileValueList.add(new FileValue(workingDirectory.toURI().relativize(file.toURI()).getPath()));
        }
        return fileValueList;
    }

    public static class FileValue {

        private String path;
        private CheckBox checkBox;

        public FileValue(String path) {
            this.path = path;
            checkBox = new CheckBox();
            checkBox.setSelected(true);
        }

        public CheckBox getIntegrityCheckBox() {
            return checkBox;
        }

        public String getPath() {
            return path;
        }

        @Override
        public boolean equals(Object obj) {

            if (obj instanceof FileValue) {
                FileValue fileValue = (FileValue) obj;
                return fileValue.getPath().equals(this.getPath());
            }
            return false;
        }
    }

    public JsonObject generateJsonObject(boolean workingDirectory) throws IOException, NoSuchAlgorithmException {

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("url", new JsonPrimitive(urlField.getText()));

        if (workingDirectory)
            jsonObject.add("workingDirectory", new JsonPrimitive(workingDirectoryPathField.getText()));

        JsonArray jsonArray = new JsonArray();

        for (FileValue value : table.getItems()) {
            JsonObject fileObject = new JsonObject();
            fileObject.add("path", new JsonPrimitive(value.getPath()));
            fileObject.add("hash", new JsonPrimitive(value.getIntegrityCheckBox().isSelected() ? Util.sha1(new File(this.workingDirectory, value.getPath())) : "none"));
            jsonArray.add(fileObject);
        }

        jsonObject.add("files", jsonArray);

        return jsonObject;
    }

    public void reset() {
        this.urlField.setText("");
        this.workingDirectoryPathField.setText("");
        this.table.getItems().clear();
    }

    public void setWorkingDirectory(String uri) {
        workingDirectoryPathField.setText(uri);
        workingDirectory = new File(uri);
    }

    public void setUrl(String url) {
        urlField.setText(url);
    }

    public ObservableList<FileValue> getTableItems() {
        return table.getItems();
    }

    public void loadTabFromJSON(JsonObject jsonObject)
    {
        String workingDirectory = jsonObject.get("workingDirectory").getAsString();

        this.setUrl(jsonObject.get("url").getAsString());
        this.setWorkingDirectory(workingDirectory);

        ObservableList<FileListTab.FileValue> items = this.getTableItems();

        JsonArray jsonArray = jsonObject.getAsJsonArray("files");
        Iterator<JsonElement> iterator = jsonArray.iterator();
        while (iterator.hasNext())
        {
            JsonObject fileData = iterator.next().getAsJsonObject();

            FileListTab.FileValue fileValue = new FileListTab.FileValue(fileData.get("path").getAsString());
            fileValue.getIntegrityCheckBox().setSelected(!fileData.get("hash").getAsString().equals("none"));

            items.add(fileValue);
        }
    }


}
