package com.wascardev.modpackupdater.modpackmanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.wascardev.modpackupdater.api.Util;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class FileListTab extends Tab {

    private TableView<FileValue> table;
    private File workingDirectory;

    public FileListTab(String text, File workingDirectory) {
        super(text);

        this.workingDirectory = workingDirectory;

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

        this.setContent(table);
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

    public JsonArray generateJsonArray() throws IOException, NoSuchAlgorithmException {
        JsonArray jsonArray = new JsonArray();

        for (FileValue value : table.getItems()) {
            JsonObject fileObject = new JsonObject();
            fileObject.add("path", new JsonPrimitive(value.getPath()));
            fileObject.add("hash", new JsonPrimitive(value.getIntegrityCheckBox().isSelected() ? Util.sha512(new File(this.workingDirectory, value.getPath())) : "none"));
            jsonArray.add(fileObject);
        }

        return jsonArray;
    }

    public ObservableList<FileValue> getTableItems() {
        return table.getItems();
    }

    public void loadTabFromJSON(JsonArray jsonArray)
    {
        ObservableList<FileListTab.FileValue> items = this.getTableItems();

        Iterator<JsonElement> iterator = jsonArray.iterator();
        while (iterator.hasNext())
        {
            JsonObject fileData = iterator.next().getAsJsonObject();

            FileListTab.FileValue fileValue = new FileListTab.FileValue(fileData.get("path").getAsString());
            fileValue.getIntegrityCheckBox().setSelected(!fileData.get("hash").getAsString().equals("none"));

            items.add(fileValue);
        }
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
