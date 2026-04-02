package com.app.plugin.i18n;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import com.app.infrastructure.i18n.I18nService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TranslationEditor extends VBox {

    private final TableView<TranslationEntry> table;
    private final ComboBox<Locale> localeSelector;
    private Locale currentEditingLocale;
    private final Button btnSave;
    private final Button btnDeleteLang;
    private final Button btnReset;

    public TranslationEditor() {
        setSpacing(15);
        setPadding(new Insets(20));
        getStyleClass().add("translation-editor");

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("Éditeur de Traductions");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        localeSelector = new ComboBox<>();
        localeSelector.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Locale object) {
                return object == null ? "" : object.getLanguage() + " - " + object.getDisplayName();
            }
            @Override
            public Locale fromString(String string) { return null; }
        });
        localeSelector.getItems().setAll(I18nService.getInstance().getAvailableLocales());
        localeSelector.setValue(I18nService.getInstance().localeProperty().get());
        localeSelector.setOnAction(e -> loadDataForLocale(localeSelector.getValue()));
        currentEditingLocale = localeSelector.getValue();

        Button btnAddLang = new Button("➕ Nouvelle Langue");
        btnAddLang.setOnAction(e -> handleAddLanguage());

        btnDeleteLang = new Button("🗑️ Suppr. Langue");
        btnDeleteLang.setStyle("-fx-text-fill: red;");
        btnDeleteLang.setOnAction(e -> handleDeleteLanguage());
        
        btnReset = new Button("♻️ Restaurer");
        btnReset.setStyle("-fx-text-fill: orange;");
        btnReset.setOnAction(e -> handleReset());

        btnSave = new Button("💾 Sauvegarder");
        btnSave.getStyleClass().add("success-button");
        btnSave.setOnAction(e -> handleSave());

        toolbar.getChildren().addAll(lblTitle, new Separator(javafx.geometry.Orientation.VERTICAL), 
                                     new Label("Langue :"), localeSelector, btnAddLang, btnDeleteLang, btnReset,
                                     new Region(), btnSave);
        HBox.setHgrow(toolbar.getChildren().get(7), Priority.ALWAYS);

        table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<TranslationEntry, String> keyCol = new TableColumn<>("Clé");
        keyCol.setCellValueFactory(data -> data.getValue().keyProperty());
        keyCol.setEditable(false);

        TableColumn<TranslationEntry, String> valueCol = new TableColumn<>("Valeur");
        valueCol.setCellValueFactory(data -> data.getValue().valueProperty());
        valueCol.setCellFactory(TextFieldTableCell.forTableColumn());
        valueCol.setOnEditCommit(event -> {
            if (isSystemLocale(currentEditingLocale)) {
                new Alert(Alert.AlertType.WARNING, "Modification interdite pour les langues système (fr/en).").show();
                table.refresh();
                return;
            }

            String oldText = event.getOldValue();
            String newText = event.getNewValue();
            
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{\\d+\\}").matcher(oldText);
            while (m.find()) {
                String var = m.group();
                if (!newText.contains(var)) {
                   Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
                        "La variable " + var + " est manquante.\nCela peut créer des erreurs.\nVoulez-vous vraiment continuer ?",
                        ButtonType.YES, ButtonType.NO);
                   alert.showAndWait().ifPresent(resp -> {
                       if (resp == ButtonType.YES) {
                           event.getRowValue().setValue(newText);
                       } else {
                           table.refresh();
                       }
                   });
                   return;
                }
            }
            event.getRowValue().setValue(newText);
        });
         valueCol.setEditable(true);

        table.getColumns().addAll(keyCol, valueCol);

        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        
        Button btnAddKey = new Button("➕ Ajouter une clé");
        btnAddKey.setOnAction(e -> handleAddKey());
        
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Rechercher une clé...");
        searchField.textProperty().addListener((obs, old, neu) -> filterTable(neu));

        bottomBar.getChildren().addAll(btnAddKey, new Separator(javafx.geometry.Orientation.VERTICAL), searchField);

        getChildren().addAll(toolbar, table, bottomBar);

        loadDataForLocale(currentEditingLocale);
    }

    private void loadDataForLocale(Locale locale) {
        if (locale == null) return;
        currentEditingLocale = locale;
        updateUIState(locale);
        
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("com.app.i18n.messages", locale, new UTF8Control());
            ObservableList<TranslationEntry> data = FXCollections.observableArrayList();
            for (String key : bundle.keySet()) {
                data.add(new TranslationEntry(key, bundle.getString(key)));
            }
            table.setItems(data);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur chargement langue: " + e.getMessage()).show();
        }
    }

    private void updateUIState(Locale locale) {
        boolean isSystem = isSystemLocale(locale);
        btnDeleteLang.setDisable(isSystem);
        btnReset.setDisable(!isSystem);
        btnSave.setDisable(isSystem);
    }

    private boolean isSystemLocale(Locale locale) {
        if (locale == null) return false;
        return locale.equals(Locale.FRENCH) || locale.equals(Locale.ENGLISH);
    }
    
    private void handleSave() {
        if (isSystemLocale(currentEditingLocale)) {
            new Alert(Alert.AlertType.WARNING, "Impossible de sauvegarder les langues système.").show();
            return;
        }
        try {
            Map<String, String> map = table.getItems().stream()
                .collect(Collectors.toMap(TranslationEntry::getKey, TranslationEntry::getValue));
            
            I18nService.getInstance().saveKeys(currentEditingLocale, map);
            
            new Alert(Alert.AlertType.INFORMATION, "Sauvegarde réussie !").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur sauvegarde: " + e.getMessage()).show();
        }
    }

    private void handleReset() {
        if (!isSystemLocale(currentEditingLocale)) {
             return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
            "Voulez-vous restaurer les valeurs par défaut pour " + currentEditingLocale + " ?\nCela écrasera vos modifications locales.", 
            ButtonType.YES, ButtonType.CANCEL);
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                try {
                    I18nService.getInstance().resetLocale(currentEditingLocale);
                    loadDataForLocale(currentEditingLocale);
                    new Alert(Alert.AlertType.INFORMATION, "Langue restaurée avec succès.").show();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Erreur restauration: " + e.getMessage()).show();
                }
            }
        });
    }

    private void handleAddLanguage() {
        TextInputDialog dialog = new TextInputDialog("es");
        dialog.setTitle("Nouvelle Langue");
        dialog.setHeaderText("Créer une nouvelle traduction");
        dialog.setContentText("Code langue (ex: es, de, it) :");

        dialog.showAndWait().ifPresent(code -> {
            try {
                I18nService.getInstance().createNewLocale(code);
                Locale newLoc = new Locale(code);
                if (!localeSelector.getItems().contains(newLoc)) {
                    localeSelector.getItems().add(newLoc);
                }
                localeSelector.setValue(newLoc);
                loadDataForLocale(newLoc);
                new Alert(Alert.AlertType.INFORMATION, "Fichier créé : messages_" + code + ".properties").show();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Erreur création: " + e.getMessage()).show();
            }
        });
    }

    private void handleDeleteLanguage() {
        Locale selected = localeSelector.getValue();
        if (selected == null) return;
        
        if (isSystemLocale(selected)) {
            new Alert(Alert.AlertType.WARNING, "Impossible de supprimer les langues par défaut (fr/en).").show();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
            "Êtes-vous sûr de vouloir supprimer la langue : " + selected.getDisplayName() + " [" + selected.getLanguage() + "] ?\nCette action est irréversible.", 
            ButtonType.YES, ButtonType.CANCEL);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    I18nService.getInstance().deleteLocale(selected);
                    localeSelector.getItems().remove(selected);
                    localeSelector.setValue(Locale.FRENCH);
                    new Alert(Alert.AlertType.INFORMATION, "Langue supprimée avec succès.").show();
                } catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Erreur suppression: " + e.getMessage()).show();
                }
            }
        });
    }

    private void handleAddKey() {
        if (isSystemLocale(currentEditingLocale)) {
             new Alert(Alert.AlertType.WARNING, "Impossible d'ajouter des clés aux langues système.").show();
             return;
        }
        Dialog<TranslationEntry> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une clé");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox content = new VBox(10);
        TextField keyField = new TextField();
        keyField.setPromptText("Ex: my.new.key");
        TextField valueField = new TextField();
        valueField.setPromptText("Valeur");
        
        content.getChildren().addAll(new Label("Clé :"), keyField, new Label("Valeur :"), valueField);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && !keyField.getText().isEmpty()) {
                return new TranslationEntry(keyField.getText(), valueField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(entry -> {
            table.getItems().add(0, entry);
            table.scrollTo(0);
            table.getSelectionModel().select(0);
        });
    }

    private void filterTable(String query) {
        if (query == null || query.isEmpty()) return;
        
        for (TranslationEntry item : table.getItems()) {
            if (item.getKey().contains(query) || item.getValue().contains(query)) {
                table.getSelectionModel().select(item);
                table.scrollTo(item);
                break;
            }
        }
    }

    public static class TranslationEntry {
        private final SimpleStringProperty key;
        private final SimpleStringProperty value;

        public TranslationEntry(String key, String value) {
            this.key = new SimpleStringProperty(key);
            this.value = new SimpleStringProperty(value);
        }
        
        public String getKey() { return key.get(); }
        public SimpleStringProperty keyProperty() { return key; }
        
        public String getValue() { return value.get(); }
        public void setValue(String v) { this.value.set(v); }
        public SimpleStringProperty valueProperty() { return value; }
    }
    
    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, java.io.IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            java.io.InputStream stream = loader.getResourceAsStream(resourceName);
            if (stream != null) {
                try (java.io.InputStreamReader reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                    return new java.util.PropertyResourceBundle(reader);
                }
            }
            return super.newBundle(baseName, locale, format, loader, reload);
        }
    }
}
