package net.alan.senlo.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import net.alan.senlo.config.Config;
import net.alan.senlo.model.ApplicationSettings;
import net.alan.senlo.model.ConversionTask;
import net.alan.senlo.service.Jave2ConversionService;
import net.alan.senlo.util.FFmpegChecker;
import net.alan.senlo.gui.MainWindow;
import net.alan.senlo.gui.ParamPanelBuilder;
import net.alan.senlo.gui.PreviewPaneBuilder;
import net.alan.senlo.util.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final MainWindow view;
    private final Jave2ConversionService conversionService;
    private final ObservableList<ConversionTask> taskQueue;
    private final ApplicationSettings applicationSettings;

    private ExecutorService threadPool;
    private Semaphore semaphore;
    private volatile boolean isRunning = false;
    private final List<ConversionTask> activeTasks = Collections.synchronizedList(new ArrayList<>());

    private TaskClipboard clipboard = new TaskClipboard();
    private Runnable paramChangeHandler;

    public MainController(MainWindow view) {
        this.view = view;
        this.conversionService = new Jave2ConversionService();
        this.taskQueue = FXCollections.observableArrayList();
        this.applicationSettings = ApplicationSettings.getInstance();

        view.getFileListView().setItems(taskQueue);
        view.getOutputDirField().setText(applicationSettings.getOutputDirectory());

        initThreadPool();
        setupListeners();

        paramChangeHandler = this::syncSelectedTasks;
    }

    private void initThreadPool() {
        int maxConcurrent = applicationSettings.getMaxConcurrentTasks();
        threadPool = Executors.newFixedThreadPool(maxConcurrent);
        semaphore = new Semaphore(maxConcurrent);
    }

    private void restartExecutor() {
        isRunning = false;
        synchronized (activeTasks) {
            for (ConversionTask task : activeTasks) {
                if (task.getFfmpegProcess() != null && task.getFfmpegProcess().isAlive()) {
                    try {
                        task.getFfmpegProcess().destroy();
                    } catch (Exception ignored) {}
                }
            }
        }
        activeTasks.clear();
        if (threadPool != null) {
            threadPool.shutdownNow();
            try {
                threadPool.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        initThreadPool();
    }

    private void setupListeners() {
        view.getFileListView().getSelectionModel().selectedItemProperty().addListener((obs, oldTask, newTask) -> {
            PreviewPaneBuilder.releaseCurrentMedia();
            if (newTask == null) {
                view.getParamContainer().getChildren().clear();
                view.getPreviewContainer().getChildren().clear();
                return;
            }
            view.getPreviewContainer().getChildren().setAll(PreviewPaneBuilder.buildPreviewPane(newTask));
            view.getParamContainer().getChildren().setAll(ParamPanelBuilder.buildPanel(newTask, paramChangeHandler));
        });

        view.setDragDropHandler(files -> {
            for (File f : files) {
                if (f.isFile()) addFileToQueue(f);
                else if (f.isDirectory()) addFolderToQueue(f);
            }
        });

        view.getBrowseButton().setOnAction(e -> chooseOutputDirectory());
        view.getOpenFolderButton().setOnAction(e -> openOutputFolder());
        view.getStartButton().setOnAction(e -> startConversion());
        view.getStopButton().setOnAction(e -> stopAllTasks());

        view.getAddFilesItem().setOnAction(e -> showAddFilesDialog());
        view.getAddFolderItem().setOnAction(e -> showAddFolderDialog());
        view.getRemoveSelectedItem().setOnAction(e -> removeSelectedTasks());
        view.getClearAllItem().setOnAction(e -> clearQueue());
        view.getImportQueueItem().setOnAction(e -> importQueue());
        view.getExportQueueItem().setOnAction(e -> exportQueue());
        view.getExitItem().setOnAction(e -> Platform.exit());

        view.getSelectAllItem().setOnAction(e -> view.getFileListView().getSelectionModel().selectAll());
        view.getInvertSelectionItem().setOnAction(e -> invertSelection());
        view.getCopyTaskItem().setOnAction(e -> copyTaskConfig());
        view.getPasteTaskItem().setOnAction(e -> pasteTaskConfig());
        view.getMoveUpItem().setOnAction(e -> moveUpSelected());
        view.getMoveDownItem().setOnAction(e -> moveDownSelected());

        view.getContextMoveUp().setOnAction(e -> moveUpSelected());
        view.getContextMoveDown().setOnAction(e -> moveDownSelected());
        view.getContextRemove().setOnAction(e -> removeSelectedTasks());
        view.getContextReset().setOnAction(e -> resetSelectedTasks());
        view.getContextRetry().setOnAction(e -> retryFailedTasks());

        view.getStartConvertItem().setOnAction(e -> startConversion());
        view.getStopAllItem().setOnAction(e -> stopAllTasks());
        view.getRetryFailedItem().setOnAction(e -> retryFailedTasks());

        view.getSettingsItem().setOnAction(e -> showSettingsDialog());
        view.getCheckFFmpegItem().setOnAction(e -> checkFFmpegEnvironment());
        view.getPresetsItem().setOnAction(e -> showInfo(Language.get("presets.title"), Language.get("presets.developing")));

        view.getDocsItem().setOnAction(e -> showInfo(Language.get("help.title"), Language.get("help.content")));
        view.getAboutItem().setOnAction(e -> AboutAPP.showAboutDialog());

        // 语言切换
        view.getLanguageZhItem().setOnAction(e -> changeLanguage("zh_cn"));
        view.getLanguageZhTwItem().setOnAction(e -> changeLanguage("zh_tw"));
        view.getLanguageEnItem().setOnAction(e -> changeLanguage("en_us"));
    }

    private void syncSelectedTasks() {
        ObservableList<ConversionTask> selectedTasks = view.getFileListView().getSelectionModel().getSelectedItems();
        ConversionTask current = view.getFileListView().getSelectionModel().getSelectedItem();
        if (selectedTasks.size() > 1 && current != null) {
            for (ConversionTask task : selectedTasks) {
                if (task == current) continue;
                if (Objects.equals(task.getMediaType(), current.getMediaType())) {
                    task.setOutputFormat(current.getOutputFormat());
                    if (current.getOptions() != null) {
                        task.setOptions(new HashMap<>(current.getOptions()));
                    }
                }
            }
            view.getFileListView().refresh();
        }
    }

    private void changeLanguage(String langCode) {
        applicationSettings.setLanguage(langCode);
        applicationSettings.save();
        Language.load();
        view.refreshTexts();
        refreshAllTaskStatuses();
        ConversionTask selected = view.getFileListView().getSelectionModel().getSelectedItem();
        if (selected != null) {
            view.getParamContainer().getChildren().setAll(ParamPanelBuilder.buildPanel(selected, paramChangeHandler));
            view.getPreviewContainer().getChildren().setAll(PreviewPaneBuilder.buildPreviewPane(selected));
        }
        view.getFileListView().refresh();
    }

    private void refreshAllTaskStatuses() {
        Map<String, String> statusKeyMap = new HashMap<>();
        statusKeyMap.put(Language.get("status.waiting"), "waiting");
        statusKeyMap.put(Language.get("status.queued"), "queued");
        statusKeyMap.put(Language.get("status.converting"), "converting");
        statusKeyMap.put(Language.get("status.completed"), "completed");
        statusKeyMap.put(Language.get("status.failed"), "failed");
        statusKeyMap.put(Language.get("status.stopped"), "stopped");
        statusKeyMap.put(Language.get("status.skipped"), "skipped");

        for (ConversionTask task : taskQueue) {
            String oldStatus = task.getStatus();
            String key = statusKeyMap.get(oldStatus);
            if (key != null) {
                task.setStatus(Language.get("status." + key));
            }
        }
    }

    private void addFileToQueue(File file) {
        ConversionTask task = new ConversionTask(file.getAbsolutePath());
        task.setOutputFormat(applicationSettings.getDefaultOutputFormat().toUpperCase());
        taskQueue.add(task);
    }

    private void addFolderToQueue(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    String name = f.getName();
                    int dotIdx = name.lastIndexOf('.');
                    if (dotIdx != -1) {
                        String ext = name.substring(dotIdx + 1).toLowerCase();
                        if (Arrays.asList(Config.SUPPORTED_EXTENSIONS).contains("*." + ext)) {
                            addFileToQueue(f);
                        }
                    }
                }
            }
        }
    }

    private void showAddFilesDialog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Language.get("dialog.addFiles.title"));
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(Language.get("dialog.mediaFiles"), Config.SUPPORTED_EXTENSIONS);
        chooser.getExtensionFilters().add(filter);
        List<File> files = chooser.showOpenMultipleDialog(view.getRoot().getScene().getWindow());
        if (files != null) {
            files.forEach(this::addFileToQueue);
        }
    }

    private void showAddFolderDialog() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(Language.get("dialog.addFolder.title"));
        File folder = chooser.showDialog(view.getRoot().getScene().getWindow());
        if (folder != null) {
            addFolderToQueue(folder);
        }
    }

    private void removeSelectedTasks() {
        List<ConversionTask> selected = new ArrayList<>(view.getFileListView().getSelectionModel().getSelectedItems());
        selected.forEach(task -> {
            if (task.getFfmpegProcess() != null && task.getFfmpegProcess().isAlive()) {
                try {
                    task.getFfmpegProcess().destroy();
                } catch (Exception ignored) {}
            }
            taskQueue.remove(task);
        });
        view.getFileListView().getSelectionModel().clearSelection();
        if (taskQueue.isEmpty()) {
            PreviewPaneBuilder.releaseCurrentMedia();
            view.getPreviewContainer().getChildren().clear();
            view.getParamContainer().getChildren().clear();
        }
    }

    private void clearQueue() {
        taskQueue.forEach(task -> {
            if (task.getFfmpegProcess() != null && task.getFfmpegProcess().isAlive()) {
                try {
                    task.getFfmpegProcess().destroy();
                } catch (Exception ignored) {}
            }
        });
        taskQueue.clear();
        PreviewPaneBuilder.releaseCurrentMedia();
        view.getParamContainer().getChildren().clear();
        view.getPreviewContainer().getChildren().clear();
    }

    private void resetSelectedTasks() {
        view.getFileListView().getSelectionModel().getSelectedItems().forEach(task -> {
            if (!Language.get("status.converting").equals(task.getStatus())) {
                task.setStatus(Language.get("status.waiting"));
                task.setProgress(0);
            }
        });
        view.getFileListView().refresh();
    }

    private void retryFailedTasks() {
        taskQueue.forEach(task -> {
            if (Language.get("status.failed").equals(task.getStatus())) {
                task.setStatus(Language.get("status.waiting"));
                task.setProgress(0);
            }
        });
        startConversion();
    }

    private void invertSelection() {
        var sm = view.getFileListView().getSelectionModel();
        List<Integer> currentSelected = new ArrayList<>(sm.getSelectedIndices());
        sm.clearSelection();
        for (int i = 0; i < taskQueue.size(); i++) {
            if (!currentSelected.contains(i)) {
                sm.select(i);
            }
        }
    }

    private void copyTaskConfig() {
        ConversionTask current = view.getFileListView().getSelectionModel().getSelectedItem();
        if (current != null) {
            List<ConversionTask> list = new ArrayList<>();
            list.add(current);
            clipboard.setTasks(list);
        }
    }

    private void pasteTaskConfig() {
        List<ConversionTask> source = clipboard.getTasks();
        List<ConversionTask> targets = view.getFileListView().getSelectionModel().getSelectedItems();
        if (source != null && !source.isEmpty() && !targets.isEmpty()) {
            ConversionTask src = source.get(0);
            for (ConversionTask target : targets) {
                if (Objects.equals(target.getMediaType(), src.getMediaType())) {
                    target.setOutputFormat(src.getOutputFormat());
                    if (src.getOptions() != null) {
                        target.setOptions(new HashMap<>(src.getOptions()));
                    }
                }
            }
            view.getFileListView().refresh();
        }
    }

    private void moveUpSelected() {
        var sm = view.getFileListView().getSelectionModel();
        List<Integer> indices = new ArrayList<>(sm.getSelectedIndices());
        Collections.sort(indices);
        for (int idx : indices) {
            if (idx > 0 && !indices.contains(idx - 1)) {
                ConversionTask task = taskQueue.remove(idx);
                taskQueue.add(idx - 1, task);
                sm.select(idx - 1);
            }
        }
    }

    private void moveDownSelected() {
        var sm = view.getFileListView().getSelectionModel();
        List<Integer> indices = new ArrayList<>(sm.getSelectedIndices());
        indices.sort(Collections.reverseOrder());
        for (int idx : indices) {
            if (idx < taskQueue.size() - 1 && !indices.contains(idx + 1)) {
                ConversionTask task = taskQueue.remove(idx);
                taskQueue.add(idx + 1, task);
                sm.select(idx + 1);
            }
        }
    }

    private void chooseOutputDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(Language.get("dialog.outputDir.title"));
        File dir = chooser.showDialog(view.getRoot().getScene().getWindow());
        if (dir != null) {
            applicationSettings.setOutputDirectory(dir.getAbsolutePath());
            applicationSettings.save();
            view.getOutputDirField().setText(dir.getAbsolutePath());
        }
    }

    private void openOutputFolder() {
        try {
            File f = new File(applicationSettings.getOutputDirectory());
            if (!f.exists()) f.mkdirs();
            Desktop.getDesktop().open(f);
        } catch (IOException e) {
            logger.error("打开输出路径失败", e);
        }
    }

    private void startConversion() {
        if (isRunning) return;
        if (!FFmpegChecker.isAvailable()) {
            showAlert(Alert.AlertType.ERROR, Language.get("alert.ffmpeg.missing.title"),
                    Language.get("alert.ffmpeg.missing.header"), Language.get("alert.ffmpeg.missing.content"));
            return;
        }

        isRunning = true;
        new Thread(() -> {
            AtomicInteger activeThreadCount = new AtomicInteger(0);
            while (isRunning) {
                ConversionTask nextTask = null;
                synchronized (taskQueue) {
                    for (ConversionTask task : taskQueue) {
                        if (Language.get("status.waiting").equals(task.getStatus())) {
                            nextTask = task;
                            nextTask.setStatus(Language.get("status.queued"));
                            break;
                        }
                    }
                }

                if (nextTask == null) {
                    if (activeThreadCount.get() == 0) {
                        isRunning = false;
                        Platform.runLater(() -> logger.info("所有队列任务执行完毕"));
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }

                final ConversionTask taskToRun = nextTask;
                try {
                    semaphore.acquire();
                    activeThreadCount.incrementAndGet();
                    activeTasks.add(taskToRun);

                    threadPool.submit(() -> {
                        try {
                            taskToRun.setStatus(Language.get("status.converting"));
                            taskToRun.setOutputPath(conversionService.buildOutputPath(taskToRun));

                            File targetFile = new File(taskToRun.getOutputPath());
                            if (targetFile.exists()) {
                                var strategy = applicationSettings.getConflictStrategy();
                                if (strategy == ApplicationSettings.ConflictStrategy.SKIP) {
                                    taskToRun.setStatus(Language.get("status.skipped"));
                                    taskToRun.setProgress(1.0);
                                    return;
                                } else if (strategy == ApplicationSettings.ConflictStrategy.RENAME) {
                                    taskToRun.setOutputPath(autoRename(taskToRun.getOutputPath()));
                                }
                            }

                            boolean success = conversionService.convert(taskToRun, progress ->
                                    Platform.runLater(() -> taskToRun.setProgress(progress))
                            );

                            Platform.runLater(() -> {
                                if (success) {
                                    taskToRun.setStatus(Language.get("status.completed"));
                                    taskToRun.setProgress(1.0);
                                } else {
                                    taskToRun.setStatus(Language.get("status.failed"));
                                }
                            });
                        } catch (Exception ex) {
                            logger.error("任务执行遇到异常", ex);
                            Platform.runLater(() -> taskToRun.setStatus(Language.get("status.failed")));
                        } finally {
                            activeTasks.remove(taskToRun);
                            activeThreadCount.decrementAndGet();
                            semaphore.release();
                        }
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    isRunning = false;
                }
            }
        }).start();
    }

    private void stopAllTasks() {
        isRunning = false;
        synchronized (taskQueue) {
            taskQueue.forEach(task -> {
                if (Language.get("status.converting").equals(task.getStatus()) || Language.get("status.queued").equals(task.getStatus())) {
                    if (task.getFfmpegProcess() != null && task.getFfmpegProcess().isAlive()) {
                        try {
                            task.getFfmpegProcess().destroy();
                        } catch (Exception ignored) {}
                    }
                    task.setStatus(Language.get("status.stopped"));
                }
            });
        }
        activeTasks.clear();
    }

    private void checkFFmpegEnvironment() {
        boolean ok = FFmpegChecker.isAvailable();
        if (ok) {
            showInfo(Language.get("ffmpeg.check.success.title"), Language.get("ffmpeg.check.success.content"));
        } else {
            showAlert(Alert.AlertType.WARNING, Language.get("ffmpeg.check.fail.title"),
                    Language.get("ffmpeg.check.fail.header"), Language.get("ffmpeg.check.fail.content"));
        }
    }

    private void exportQueue() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Language.get("dialog.export.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Senlo Queue (*.json)", "*.json"));
        File file = chooser.showSaveDialog(view.getRoot().getScene().getWindow());
        if (file != null) {
            try {
                List<TaskExportData> exportList = new ArrayList<>();
                for (ConversionTask t : taskQueue) {
                    TaskExportData data = new TaskExportData();
                    data.inputPath = t.getInputPath();
                    data.outputFormat = t.getOutputFormat();
                    data.options = t.getOptions();
                    exportList.add(data);
                }
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
                    gson.toJson(exportList, writer);
                }
                showInfo(Language.get("dialog.export.success.title"), Language.get("dialog.export.success.content") + file.getName());
            } catch (Exception ex) {
                logger.error("导出队列错误", ex);
                showAlert(Alert.AlertType.ERROR, Language.get("dialog.export.error.title"), Language.get("dialog.export.error.header"), ex.getMessage());
            }
        }
    }

    private void importQueue() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(Language.get("dialog.import.title"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Senlo Queue (*.json)", "*.json"));
        File file = chooser.showOpenDialog(view.getRoot().getScene().getWindow());
        if (file != null) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<List<TaskExportData>>() {}.getType();
                List<TaskExportData> importList;
                try (Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
                    importList = gson.fromJson(reader, type);
                }
                if (importList != null) {
                    for (TaskExportData data : importList) {
                        File mediaFile = new File(data.inputPath);
                        if (mediaFile.exists()) {
                            ConversionTask task = new ConversionTask(data.inputPath);
                            task.setOutputFormat(data.outputFormat);
                            if (data.options != null) {
                                task.setOptions(new HashMap<>(data.options));
                            }
                            taskQueue.add(task);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error("导入配置故障", ex);
                showAlert(Alert.AlertType.ERROR, Language.get("dialog.import.error.title"), Language.get("dialog.import.error.header"), ex.getMessage());
            }
        }
    }

    private void showSettingsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(Language.get("settings.title"));
        dialog.setHeaderText(Language.get("settings.header"));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        dialog.getDialogPane().setStyle("-fx-background-color: #FFFFFF;");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Slider concurrentSlider = new Slider(1, 8, applicationSettings.getMaxConcurrentTasks());
        concurrentSlider.setShowTickMarks(true);
        concurrentSlider.setShowTickLabels(true);
        concurrentSlider.setMajorTickUnit(1);
        concurrentSlider.setMinorTickCount(0);
        concurrentSlider.setSnapToTicks(true);

        ComboBox<String> conflictCombo = new ComboBox<>();
        conflictCombo.getItems().addAll(Language.get("settings.conflict.rename"), Language.get("settings.conflict.overwrite"), Language.get("settings.conflict.skip"));
        var currentStrategy = applicationSettings.getConflictStrategy();
        if (currentStrategy == ApplicationSettings.ConflictStrategy.RENAME) conflictCombo.setValue(Language.get("settings.conflict.rename"));
        else if (currentStrategy == ApplicationSettings.ConflictStrategy.OVERWRITE) conflictCombo.setValue(Language.get("settings.conflict.overwrite"));
        else if (currentStrategy == ApplicationSettings.ConflictStrategy.SKIP) conflictCombo.setValue(Language.get("settings.conflict.skip"));

        grid.add(new Label(Language.get("settings.maxConcurrent")), 0, 0);
        grid.add(concurrentSlider, 1, 0);
        grid.add(new Label(Language.get("settings.conflictStrategy")), 0, 1);
        grid.add(conflictCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setOnCloseRequest(e -> {
            int newMax = (int) concurrentSlider.getValue();
            boolean needPoolReset = (newMax != applicationSettings.getMaxConcurrentTasks());
            applicationSettings.setMaxConcurrentTasks(newMax);

            String selected = conflictCombo.getValue();
            if (Language.get("settings.conflict.rename").equals(selected)) applicationSettings.setConflictStrategy(ApplicationSettings.ConflictStrategy.RENAME);
            else if (Language.get("settings.conflict.overwrite").equals(selected)) applicationSettings.setConflictStrategy(ApplicationSettings.ConflictStrategy.OVERWRITE);
            else if (Language.get("settings.conflict.skip").equals(selected)) applicationSettings.setConflictStrategy(ApplicationSettings.ConflictStrategy.SKIP);

            applicationSettings.save();
            if (needPoolReset) {
                restartExecutor();
            }
        });

        dialog.showAndWait();
    }

    private String autoRename(String outputPath) {
        File file = new File(outputPath);
        if (!file.exists()) return outputPath;
        String parent = file.getParent();
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        String baseName = (lastDot == -1) ? name : name.substring(0, lastDot);
        String extension = (lastDot == -1) ? "" : name.substring(lastDot);
        int count = 1;
        while (file.exists()) {
            file = new File(parent, baseName + " (" + count + ")" + extension);
            count++;
        }
        return file.getAbsolutePath();
    }

    private void showInfo(String title, String content) {
        showAlert(Alert.AlertType.INFORMATION, title, null, content);
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        alert.showAndWait();
    }

    private static class TaskClipboard {
        private List<ConversionTask> tasks;
        public List<ConversionTask> getTasks() { return tasks; }
        public void setTasks(List<ConversionTask> tasks) { this.tasks = tasks; }
    }

    private static class TaskExportData {
        String inputPath;
        String outputFormat;
        Map<String, Object> options;
    }
}