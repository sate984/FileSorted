package org.example.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

// Реализация пункта 2 (Модель) и 14 (Связанные объекты - данные внутри свойств)
public class SortingRule {
    private final StringProperty extension;
    private final StringProperty targetFolder;
    private final StringProperty description;

    public SortingRule(String extension, String targetFolder, String description) {
        this.extension = new SimpleStringProperty(extension);
        this.targetFolder = new SimpleStringProperty(targetFolder);
        this.description = new SimpleStringProperty(description);
    }

    // Геттеры и сеттеры свойств для JavaFX TableView
    public String getExtension() { return extension.get(); }
    public void setExtension(String value) { extension.set(value); }
    public StringProperty extensionProperty() { return extension; }

    public String getTargetFolder() { return targetFolder.get(); }
    public void setTargetFolder(String value) { targetFolder.set(value); }
    public StringProperty targetFolderProperty() { return targetFolder; }

    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }
    public StringProperty descriptionProperty() { return description; }

    // Для сохранения в CSV
    @Override
    public String toString() {
        return extension.get() + ";" + targetFolder.get() + ";" + description.get();
    }

    // Парсинг из CSV строки
    public static SortingRule fromString(String line) {
        String[] parts = line.split(";");
        if (parts.length >= 3) {
            return new SortingRule(parts[0], parts[1], parts[2]);
        }
        return null;
    }
}