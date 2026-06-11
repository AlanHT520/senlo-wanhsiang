package net.alan.senlo.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import net.alan.senlo.model.ConversionTask;
import net.alan.senlo.util.Language;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class MainWindow {
    private final BorderPane root;

    private static final String COLOR_BORDER_DARK = "#3E3E42";

    private MenuBar menuBar;
    private Menu fileMenu, editMenu, runMenu, toolsMenu, helpMenu;
    private MenuItem addFilesItem, addFolderItem, removeSelectedItem, clearAllItem, importQueueItem, exportQueueItem, exitItem;
    private MenuItem selectAllItem, invertSelectionItem, copyTaskItem, pasteTaskItem, moveUpItem, moveDownItem;
    private MenuItem startConvertItem, stopAllTasksItem, retryFailedItem;
    private MenuItem settingsItem, checkFFmpegItem, presetsItem;
    private MenuItem docsItem, aboutItem;
    private MenuItem languageZhItem, languageZhTwItem, languageEnItem;

    private TableView<ConversionTask> fileListView;
    private TextField outputDirField;
    private Button browseButton, openFolderButton, startButton, stopButton;

    private VBox rightPane;
    private VBox previewContainer;
    private VBox paramContainer;
    private Label previewTitle;
    private Label paramTitle;
    private Label queueTitle;

    private MenuItem contextMoveUp, contextMoveDown, contextRemove, contextReset, contextRetry;

    private TableColumn<ConversionTask, String> nameCol;
    private TableColumn<ConversionTask, String> formatCol;
    private TableColumn<ConversionTask, ConversionTask> progressCol;
    private TableColumn<ConversionTask, String> statusCol;

    public MainWindow() {
        root = new BorderPane();
        root.getStyleClass().add("root");

        createMenu();
        root.setTop(menuBar);

        SplitPane mainSplit = new SplitPane();
        mainSplit.getStyleClass().add("main-split-pane");

        VBox leftPane = createLeftPane();
        VBox rightPane = createRightPane();

        mainSplit.getItems().addAll(leftPane, rightPane);
        mainSplit.setDividerPositions(0.58);
        SplitPane.setResizableWithParent(leftPane, true);
        SplitPane.setResizableWithParent(rightPane, true);

        Platform.runLater(() -> {
            mainSplit.lookupAll(".split-pane-divider").forEach(node ->
                    node.setStyle("-fx-background-color: " + COLOR_BORDER_DARK + "; -fx-padding: 0 1 0 1;")
            );
        });

        AnchorPane centerWrapper = new AnchorPane(mainSplit);
        AnchorPane.setTopAnchor(mainSplit, 12.0);
        AnchorPane.setBottomAnchor(mainSplit, 4.0);
        AnchorPane.setLeftAnchor(mainSplit, 14.0);
        AnchorPane.setRightAnchor(mainSplit, 14.0);
        root.setCenter(centerWrapper);

        root.setBottom(createBottomConsole());

        refreshTexts();
        setupPlaceholder();
    }

    private void createMenu() {
        menuBar = new MenuBar();
        menuBar.getStyleClass().add("menu-bar");

        fileMenu = new Menu();
        addFilesItem = new MenuItem();
        addFolderItem = new MenuItem();
        removeSelectedItem = new MenuItem();
        clearAllItem = new MenuItem();
        importQueueItem = new MenuItem();
        exportQueueItem = new MenuItem();
        exitItem = new MenuItem();
        fileMenu.getItems().addAll(addFilesItem, addFolderItem, new SeparatorMenuItem(),
                removeSelectedItem, clearAllItem, new SeparatorMenuItem(),
                importQueueItem, exportQueueItem, new SeparatorMenuItem(), exitItem);

        editMenu = new Menu();
        selectAllItem = new MenuItem();
        invertSelectionItem = new MenuItem();
        copyTaskItem = new MenuItem();
        pasteTaskItem = new MenuItem();
        moveUpItem = new MenuItem();
        moveDownItem = new MenuItem();
        editMenu.getItems().addAll(selectAllItem, invertSelectionItem, new SeparatorMenuItem(),
                copyTaskItem, pasteTaskItem, new SeparatorMenuItem(), moveUpItem, moveDownItem);

        runMenu = new Menu();
        startConvertItem = new MenuItem();
        stopAllTasksItem = new MenuItem();
        retryFailedItem = new MenuItem();
        runMenu.getItems().addAll(startConvertItem, stopAllTasksItem, new SeparatorMenuItem(), retryFailedItem);

        toolsMenu = new Menu();
        settingsItem = new MenuItem();
        checkFFmpegItem = new MenuItem();
        presetsItem = new MenuItem();

        Menu languageMenu = new Menu();
        languageZhItem = new MenuItem();
        languageZhTwItem = new MenuItem();
        languageEnItem = new MenuItem();
        languageMenu.getItems().addAll(languageZhItem, languageZhTwItem, languageEnItem);
        toolsMenu.getItems().addAll(settingsItem, checkFFmpegItem, presetsItem, new SeparatorMenuItem(), languageMenu);

        helpMenu = new Menu();
        docsItem = new MenuItem();
        aboutItem = new MenuItem();
        helpMenu.getItems().addAll(docsItem, aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, runMenu, toolsMenu, helpMenu);
    }

    private VBox createLeftPane() {
        VBox leftPane = new VBox();
        leftPane.setStyle("-fx-padding: 0 10 0 0;");
        VBox.setVgrow(leftPane, Priority.ALWAYS);

        VBox previewSection = new VBox(6);
        previewTitle = new Label();
        previewTitle.getStyleClass().add("label-bold");
        previewContainer = new VBox(6);
        previewContainer.getStyleClass().add("flat-container");
        ScrollPane previewScroll = new ScrollPane(previewContainer);
        previewScroll.setFitToWidth(true);
        previewScroll.getStyleClass().add("transparent-scroll-pane");
        VBox.setVgrow(previewScroll, Priority.ALWAYS);
        previewSection.getChildren().addAll(previewTitle, previewScroll);
        VBox.setVgrow(previewSection, Priority.ALWAYS);

        VBox listSection = new VBox(6);
        queueTitle = new Label();
        queueTitle.getStyleClass().add("label-bold");

        fileListView = new TableView<>();
        fileListView.getStyleClass().add("table-view");
        fileListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fileListView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        fileListView.setRowFactory(tv -> {
            TableRow<ConversionTask> row = new TableRow<>();
            row.getStyleClass().add("table-row-cell");
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    row.startFullDrag();
                    if (!tv.getSelectionModel().isSelected(row.getIndex())) {
                        tv.getSelectionModel().select(row.getIndex());
                    }
                }
            });
            row.setOnMouseDragEntered(event -> {
                if (!row.isEmpty()) {
                    tv.getSelectionModel().select(row.getIndex());
                }
            });
            return row;
        });
        VBox.setVgrow(fileListView, Priority.ALWAYS);

        nameCol = new TableColumn<>();
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        nameCol.setPrefWidth(220);
        formatCol = new TableColumn<>();
        formatCol.setCellValueFactory(new PropertyValueFactory<>("outputFormat"));
        formatCol.setPrefWidth(80);
        progressCol = new TableColumn<>();
        progressCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        progressCol.setPrefWidth(120);
        progressCol.setCellFactory(column -> new TableCell<>() {
            private final ProgressBar pb = new ProgressBar();
            {
                pb.setMaxWidth(Double.MAX_VALUE);
                pb.getStyleClass().add("progress-bar");
            }
            @Override
            protected void updateItem(ConversionTask task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setGraphic(null);
                    pb.progressProperty().unbind();
                } else {
                    pb.progressProperty().unbind();
                    pb.progressProperty().bind(task.progressProperty());
                    setGraphic(pb);
                }
            }
        });
        statusCol = new TableColumn<>();
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(80);
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (Language.get("status.completed").equals(item)) setStyle("-fx-text-fill: #4CAF50;");
                    else if (Language.get("status.converting").equals(item)) setStyle("-fx-text-fill: #2196F3;");
                    else setStyle("-fx-text-fill: #E0E0E0;");
                }
            }
        });
        fileListView.getColumns().addAll(nameCol, formatCol, progressCol, statusCol);
        createTableContextMenu();

        listSection.getChildren().addAll(queueTitle, fileListView);
        VBox.setVgrow(listSection, Priority.ALWAYS);

        SplitPane leftVerticalSplit = new SplitPane();
        leftVerticalSplit.setOrientation(Orientation.VERTICAL);
        leftVerticalSplit.getStyleClass().add("vertical-split-pane");
        leftVerticalSplit.getItems().addAll(previewSection, listSection);
        leftVerticalSplit.setDividerPositions(0.70);

        Platform.runLater(() -> {
            leftVerticalSplit.lookupAll(".split-pane-divider").forEach(node ->
                    node.setStyle("-fx-background-color: " + COLOR_BORDER_DARK + "; -fx-padding: 0 1 0 1;")
            );
        });

        VBox.setVgrow(leftVerticalSplit, Priority.ALWAYS);
        leftPane.getChildren().add(leftVerticalSplit);
        return leftPane;
    }

    private void createTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        contextMoveUp = new MenuItem();
        contextMoveDown = new MenuItem();
        contextRemove = new MenuItem();
        contextReset = new MenuItem();
        contextRetry = new MenuItem();
        contextMenu.getItems().addAll(contextMoveUp, contextMoveDown, new SeparatorMenuItem(),
                contextRemove, contextReset, contextRetry);
        fileListView.setContextMenu(contextMenu);
    }

    private VBox createRightPane() {
        rightPane = new VBox(6);
        rightPane.setStyle("-fx-padding: 0 0 0 10;");
        VBox.setVgrow(rightPane, Priority.ALWAYS);
        paramTitle = new Label();
        paramTitle.getStyleClass().add("label-bold");
        paramContainer = new VBox(10);
        paramContainer.getStyleClass().add("flat-container");
        VBox.setVgrow(paramContainer, Priority.ALWAYS);
        ScrollPane paramScroll = new ScrollPane(paramContainer);
        paramScroll.setFitToWidth(true);
        paramScroll.getStyleClass().add("transparent-scroll-pane");
        VBox.setVgrow(paramScroll, Priority.ALWAYS);
        rightPane.getChildren().addAll(paramTitle, paramScroll);
        return rightPane;
    }

    private VBox createBottomConsole() {
        VBox consoleContainer = new VBox(12);
        consoleContainer.setPadding(new Insets(14, 14, 14, 14));
        consoleContainer.getStyleClass().add("bottom-console");

        HBox pathRow = new HBox(10);
        pathRow.setAlignment(Pos.CENTER_LEFT);
        Label dirLabel = new Label();
        dirLabel.getStyleClass().add("label-bold");
        outputDirField = new TextField();
        HBox.setHgrow(outputDirField, Priority.ALWAYS);
        outputDirField.setEditable(false);
        browseButton = new Button();
        openFolderButton = new Button();
        pathRow.getChildren().addAll(dirLabel, outputDirField, browseButton, openFolderButton);

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        Label tipsLabel = new Label();
        tipsLabel.getStyleClass().add("label-muted");
        Region horizontalSpacer = new Region();
        HBox.setHgrow(horizontalSpacer, Priority.ALWAYS);
        stopButton = new Button();
        startButton = new Button();
        startButton.getStyleClass().add("button-primary");
        actionRow.getChildren().addAll(tipsLabel, horizontalSpacer, stopButton, startButton);

        consoleContainer.getChildren().addAll(pathRow, actionRow);
        return consoleContainer;
    }

    private void setupPlaceholder() {
        Label placeholderLabel = new Label();
        placeholderLabel.getStyleClass().add("label-muted");
        fileListView.setPlaceholder(placeholderLabel);
    }

    public void refreshTexts() {
        // 菜单栏
        fileMenu.setText(Language.get("menu.file"));
        editMenu.setText(Language.get("menu.edit"));
        runMenu.setText(Language.get("menu.run"));
        toolsMenu.setText(Language.get("menu.tools"));
        helpMenu.setText(Language.get("menu.help"));

        addFilesItem.setText(Language.get("menu.addFiles"));
        addFolderItem.setText(Language.get("menu.addFolder"));
        removeSelectedItem.setText(Language.get("menu.removeSelected"));
        clearAllItem.setText(Language.get("menu.clearAll"));
        importQueueItem.setText(Language.get("menu.importQueue"));
        exportQueueItem.setText(Language.get("menu.exportQueue"));
        exitItem.setText(Language.get("menu.exit"));

        selectAllItem.setText(Language.get("menu.selectAll"));
        invertSelectionItem.setText(Language.get("menu.invertSelection"));
        copyTaskItem.setText(Language.get("menu.copyTask"));
        pasteTaskItem.setText(Language.get("menu.pasteTask"));
        moveUpItem.setText(Language.get("menu.moveUp"));
        moveDownItem.setText(Language.get("menu.moveDown"));

        startConvertItem.setText(Language.get("menu.startConvert"));
        stopAllTasksItem.setText(Language.get("menu.stopAll"));
        retryFailedItem.setText(Language.get("menu.retryFailed"));

        settingsItem.setText(Language.get("menu.settings"));
        checkFFmpegItem.setText(Language.get("menu.checkFFmpeg"));
        presetsItem.setText(Language.get("menu.presets"));

        docsItem.setText(Language.get("menu.docs"));
        aboutItem.setText(Language.get("menu.about"));

        // 语言菜单
        ((Menu) toolsMenu.getItems().get(toolsMenu.getItems().size() - 1)).setText(Language.get("menu.language"));
        languageZhItem.setText(Language.get("menu.language.zh"));
        languageZhTwItem.setText(Language.get("menu.language.zh_tw"));
        languageEnItem.setText(Language.get("menu.language.en"));

        // 表格列标题
        nameCol.setText(Language.get("table.column.source"));
        formatCol.setText(Language.get("table.column.format"));
        progressCol.setText(Language.get("table.column.progress"));
        statusCol.setText(Language.get("table.column.status"));

        // 队列标题
        queueTitle.setText(Language.get("table.queue.title"));

        // 预览和参数标题
        previewTitle.setText(Language.get("preview.title"));
        paramTitle.setText(Language.get("param.title"));

        // 底部控制台
        ((Label) ((HBox) ((VBox) root.getBottom()).getChildren().get(0)).getChildren().get(0)).setText(Language.get("label.outputDir"));
        browseButton.setText(Language.get("button.browse"));
        openFolderButton.setText(Language.get("button.openFolder"));
        stopButton.setText(Language.get("button.stop"));
        startButton.setText(Language.get("button.start"));
        ((Label) ((HBox) ((VBox) root.getBottom()).getChildren().get(1)).getChildren().get(0)).setText(Language.get("tips.dragDrop"));

        // 右键菜单项
        contextMoveUp.setText(Language.get("menu.moveUp"));
        contextMoveDown.setText(Language.get("menu.moveDown"));
        contextRemove.setText(Language.get("menu.removeSelected"));
        contextReset.setText(Language.get("menu.contextReset"));
        contextRetry.setText(Language.get("menu.retryFailed"));

        // 表格占位符文本
        Label placeholder = (Label) fileListView.getPlaceholder();
        if (placeholder != null) {
            placeholder.setText(Language.get("table.placeholder"));
        }
    }

    public void setDragDropHandler(Consumer<List<File>> onFilesDropped) {
        fileListView.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        fileListView.setOnDragDropped(event -> {
            var db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                onFilesDropped.accept(db.getFiles());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    // 公开 API
    public Pane getRoot() { return root; }
    public TableView<ConversionTask> getFileListView() { return fileListView; }
    public TextField getOutputDirField() { return outputDirField; }
    public Button getBrowseButton() { return browseButton; }
    public Button getOpenFolderButton() { return openFolderButton; }
    public Button getStartButton() { return startButton; }
    public Button getStopButton() { return stopButton; }
    public VBox getParamContainer() { return paramContainer; }
    public VBox getPreviewContainer() { return previewContainer; }

    public MenuItem getAddFilesItem() { return addFilesItem; }
    public MenuItem getAddFolderItem() { return addFolderItem; }
    public MenuItem getRemoveSelectedItem() { return removeSelectedItem; }
    public MenuItem getClearAllItem() { return clearAllItem; }
    public MenuItem getImportQueueItem() { return importQueueItem; }
    public MenuItem getExportQueueItem() { return exportQueueItem; }
    public MenuItem getExitItem() { return exitItem; }
    public MenuItem getSelectAllItem() { return selectAllItem; }
    public MenuItem getInvertSelectionItem() { return invertSelectionItem; }
    public MenuItem getCopyTaskItem() { return copyTaskItem; }
    public MenuItem getPasteTaskItem() { return pasteTaskItem; }
    public MenuItem getMoveUpItem() { return moveUpItem; }
    public MenuItem getMoveDownItem() { return moveDownItem; }
    public MenuItem getStartConvertItem() { return startConvertItem; }
    public MenuItem getStopAllItem() { return stopAllTasksItem; }
    public MenuItem getRetryFailedItem() { return retryFailedItem; }
    public MenuItem getSettingsItem() { return settingsItem; }
    public MenuItem getCheckFFmpegItem() { return checkFFmpegItem; }
    public MenuItem getPresetsItem() { return presetsItem; }
    public MenuItem getDocsItem() { return docsItem; }
    public MenuItem getAboutItem() { return aboutItem; }
    public MenuItem getContextMoveUp() { return contextMoveUp; }
    public MenuItem getContextMoveDown() { return contextMoveDown; }
    public MenuItem getContextRemove() { return contextRemove; }
    public MenuItem getContextReset() { return contextReset; }
    public MenuItem getContextRetry() { return contextRetry; }

    public MenuItem getLanguageZhItem() { return languageZhItem; }
    public MenuItem getLanguageZhTwItem() { return languageZhTwItem; }
    public MenuItem getLanguageEnItem() { return languageEnItem; }
}