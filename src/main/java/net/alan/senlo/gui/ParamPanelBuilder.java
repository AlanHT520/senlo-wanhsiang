package net.alan.senlo.gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import net.alan.senlo.model.ConversionTask;
import net.alan.senlo.model.FormatRegistry;
import net.alan.senlo.util.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParamPanelBuilder {

    public static VBox buildPanel(ConversionTask task, Runnable onParamChanged) {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(5, 0, 5, 0));
        panel.getStyleClass().add("param-panel");

        Label formatLabel = new Label(Language.get("param.formatLabel"));
        formatLabel.getStyleClass().add("label-bold");

        ComboBox<String> formatCombo = new ComboBox<>();
        formatCombo.setMaxWidth(Double.MAX_VALUE);

        String mediaType = task.getMediaType();
        List<String> formatNames = new ArrayList<>();
        if ("video".equals(mediaType)) {
            formatNames.addAll(FormatRegistry.getFormatNamesByCategory("video"));
            formatNames.addAll(FormatRegistry.getFormatNamesByCategory("audio"));
            formatNames.add("gif");
        } else if ("audio".equals(mediaType)) {
            formatNames.addAll(FormatRegistry.getFormatNamesByCategory("audio"));
        } else if ("image".equals(mediaType)) {
            formatNames.addAll(FormatRegistry.getFormatNamesByCategory("image"));
        } else if ("document".equals(mediaType)) {
            formatNames.addAll(FormatRegistry.getFormatNamesByCategory("document"));
        } else if ("archive".equals(mediaType)) {
            formatNames.addAll(FormatRegistry.getFormatNamesByCategory("archive"));
        } else {
            formatNames.addAll(FormatRegistry.getFormatNamesByCategory("archive"));
        }

        formatNames = formatNames.stream().distinct().toList();
        formatCombo.getItems().addAll(formatNames);
        String currentFormat = task.getOutputFormat();
        if ("GIF动画".equals(currentFormat)) currentFormat = "gif";
        if (!formatNames.contains(currentFormat) && !formatNames.isEmpty()) {
            currentFormat = formatNames.get(0);
            task.setOutputFormat(currentFormat);
        }
        formatCombo.setValue(currentFormat);

        formatCombo.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                task.setOutputFormat(val);
                if (onParamChanged != null) onParamChanged.run();
                rebuildPanel(panel, task, onParamChanged);
            }
        });

        panel.getChildren().addAll(formatLabel, formatCombo);
        Separator sep = new Separator();
        panel.getChildren().add(sep);

        Map<String, Object> opts = task.getOptions();
        String type = task.getMediaType();

        if ("video".equals(type)) {
            String selectedFormat = task.getOutputFormat().toLowerCase();
            if (selectedFormat.matches("mp3|aac|m4a|wav|flac|opus|ogg")) {
                panel.getChildren().add(createAudioGrid(task, opts, onParamChanged));
            } else if (selectedFormat.matches("gif|gif动画|jpg|jpeg|png|webp")) {
                panel.getChildren().add(createImageGrid(task, opts, onParamChanged, formatCombo));
            } else {
                panel.getChildren().add(createVideoGrid(task, opts, onParamChanged, formatCombo));
            }
        } else if ("audio".equals(type)) {
            panel.getChildren().add(createAudioGrid(task, opts, onParamChanged));
        } else if ("image".equals(type)) {
            panel.getChildren().add(createImageGrid(task, opts, onParamChanged, formatCombo));
        } else if ("archive".equals(type)) {
            Label hint = new Label(Language.get("param.archiveHint"));
            hint.setWrapText(true);
            hint.getStyleClass().add("label-muted");
            panel.getChildren().add(hint);
        } else {
            Label fallback = new Label(Language.get("param.fallback"));
            fallback.setWrapText(true);
            fallback.getStyleClass().add("label-muted");
            panel.getChildren().add(fallback);
        }

        return panel;
    }

    private static void rebuildPanel(VBox panel, ConversionTask task, Runnable onParamChanged) {
        if (panel.getChildren().size() > 2) {
            panel.getChildren().remove(2, panel.getChildren().size());
        }
        Map<String, Object> opts = task.getOptions();
        String type = task.getMediaType();

        if ("video".equals(type)) {
            String selectedFormat = task.getOutputFormat().toLowerCase();
            if (selectedFormat.matches("mp3|aac|m4a|wav|flac|opus|ogg")) {
                panel.getChildren().add(createAudioGrid(task, opts, onParamChanged));
            } else if (selectedFormat.matches("gif|gif动画|jpg|jpeg|png|webp")) {
                @SuppressWarnings("unchecked")
                ComboBox<String> formatCombo = (ComboBox<String>) panel.getChildren().get(1);
                panel.getChildren().add(createImageGrid(task, opts, onParamChanged, formatCombo));
            } else {
                @SuppressWarnings("unchecked")
                ComboBox<String> formatCombo = (ComboBox<String>) panel.getChildren().get(1);
                panel.getChildren().add(createVideoGrid(task, opts, onParamChanged, formatCombo));
            }
        } else if ("audio".equals(type)) {
            panel.getChildren().add(createAudioGrid(task, opts, onParamChanged));
        } else if ("image".equals(type)) {
            @SuppressWarnings("unchecked")
            ComboBox<String> formatCombo = (ComboBox<String>) panel.getChildren().get(1);
            panel.getChildren().add(createImageGrid(task, opts, onParamChanged, formatCombo));
        } else if ("archive".equals(type)) {
            Label hint = new Label(Language.get("param.archiveHint"));
            hint.setWrapText(true);
            hint.getStyleClass().add("label-muted");
            panel.getChildren().add(hint);
        }
    }

    private static GridPane createVideoGrid(ConversionTask task, Map<String, Object> opts, Runnable onParamChanged, ComboBox<String> formatCombo) {
        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(10);
        grid.setPadding(new Insets(5, 0, 5, 0));
        int row = 0;

        String selectedFormat = task.getOutputFormat().toLowerCase();
        boolean isGif = selectedFormat.equals("gif");

        if (isGif) {
            Label fpsLabel = new Label(Language.get("param.gif.fps"));
            grid.add(fpsLabel, 0, row);
            double currentFps = ((Number) opts.getOrDefault("gifFps", 10.0)).doubleValue();
            Slider fpsSlider = new Slider(1, 30, currentFps);
            fpsSlider.setShowTickLabels(true);
            fpsSlider.setMajorTickUnit(5);
            fpsSlider.setMinorTickCount(1);
            fpsSlider.setSnapToTicks(true);
            Label fpsVal = new Label(String.format("%.1f", currentFps));
            fpsSlider.valueProperty().addListener((obs, old, val) -> {
                double v = val.doubleValue();
                task.setOption("gifFps", v);
                fpsVal.setText(String.format("%.1f", v));
                if (onParamChanged != null) onParamChanged.run();
            });
            HBox fpsBox = new HBox(6, fpsSlider, fpsVal);
            grid.add(fpsBox, 1, row++);
        } else {
            Label vcodecLabel = new Label(Language.get("param.videoCodec"));
            grid.add(vcodecLabel, 0, row);
            ComboBox<String> vCodec = new ComboBox<>();
            vCodec.getItems().addAll(
                    Language.get("param.videoCodec.h264"),
                    Language.get("param.videoCodec.hevc"),
                    Language.get("param.videoCodec.vp9"),
                    Language.get("param.videoCodec.copy")
            );
            String currentCodec = (String) opts.getOrDefault("videoCodec", Language.get("param.videoCodec.h264"));
            vCodec.setValue(currentCodec);
            vCodec.valueProperty().addListener((obs, old, val) -> {
                task.setOption("videoCodec", val);
                if (onParamChanged != null) onParamChanged.run();
            });
            grid.add(vCodec, 1, row++);
        }

        Label resLabel = new Label(Language.get("param.resolution"));
        grid.add(resLabel, 0, row);
        ComboBox<String> res = new ComboBox<>();
        res.getItems().addAll(
                Language.get("param.resolution.original"),
                "1920x1080", "1280x720", "3840x2160"
        );
        if (!isGif) {
            String currentRes = (String) opts.getOrDefault("resolution", Language.get("param.resolution.original"));
            res.setValue(currentRes);
            res.valueProperty().addListener((obs, old, val) -> {
                task.setOption("resolution", val);
                if (onParamChanged != null) onParamChanged.run();
            });
        } else {
            res.setValue(Language.get("param.resolution.original"));
            res.setDisable(true);
        }
        grid.add(res, 1, row++);

        if (!isGif) {
            Label crfLabel = new Label(Language.get("param.crf"));
            grid.add(crfLabel, 0, row);
            int currentCrf = ((Number) opts.getOrDefault("crf", 23)).intValue();
            Slider crfSlider = new Slider(0, 51, currentCrf);
            crfSlider.setShowTickLabels(true);
            crfSlider.setMajorTickUnit(10);
            Label crfVal = new Label(String.valueOf(currentCrf));
            crfSlider.valueProperty().addListener((obs, old, val) -> {
                task.setOption("crf", val.intValue());
                crfVal.setText(String.valueOf(val.intValue()));
                if (onParamChanged != null) onParamChanged.run();
            });
            HBox crfBox = new HBox(6, crfSlider, crfVal);
            grid.add(crfBox, 1, row++);
        }

        if (isGif) {
            Label scaleLabel = new Label(Language.get("param.scaleMode"));
            grid.add(scaleLabel, 0, row);
            ComboBox<String> scaleMode = new ComboBox<>();
            scaleMode.getItems().addAll(Language.get("param.scale.keep"), Language.get("param.scale.custom"));
            String currentScale = (String) opts.getOrDefault("scaleMode", Language.get("param.scale.keep"));
            scaleMode.setValue(currentScale);
            scaleMode.valueProperty().addListener((obs, old, val) -> {
                task.setOption("scaleMode", val);
                if (onParamChanged != null) onParamChanged.run();
            });
            grid.add(scaleMode, 1, row++);

            HBox scaleBox = new HBox(4);
            TextField scaleWidth = new TextField(); scaleWidth.setPromptText(Language.get("param.width"));
            TextField scaleHeight = new TextField(); scaleHeight.setPromptText(Language.get("param.height"));
            scaleWidth.setPrefWidth(55);
            scaleHeight.setPrefWidth(55);
            scaleBox.getChildren().addAll(scaleWidth, new Label("×"), scaleHeight);
            scaleBox.setVisible(Language.get("param.scale.custom").equals(scaleMode.getValue()));
            grid.add(scaleBox, 1, row++);

            scaleMode.valueProperty().addListener((obs, old, val) -> {
                scaleBox.setVisible(Language.get("param.scale.custom").equals(val));
                if (onParamChanged != null) onParamChanged.run();
            });

            Runnable updateCustomSize = () -> {
                if (Language.get("param.scale.custom").equals(scaleMode.getValue()) && !scaleWidth.getText().isEmpty() && !scaleHeight.getText().isEmpty()) {
                    task.setOption("customSize", scaleWidth.getText() + "x" + scaleHeight.getText());
                    if (onParamChanged != null) onParamChanged.run();
                }
            };
            scaleWidth.textProperty().addListener((obs, old, val) -> updateCustomSize.run());
            scaleHeight.textProperty().addListener((obs, old, val) -> updateCustomSize.run());
        }

        return grid;
    }

    private static GridPane createAudioGrid(ConversionTask task, Map<String, Object> opts, Runnable onParamChanged) {
        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(10);
        grid.setPadding(new Insets(5, 0, 5, 0));
        int row = 0;

        Label acodecLabel = new Label(Language.get("param.audioCodec"));
        grid.add(acodecLabel, 0, row);
        ComboBox<String> aCodec = new ComboBox<>();
        aCodec.getItems().addAll(
                Language.get("param.audioCodec.mp3"),
                Language.get("param.audioCodec.aac"),
                Language.get("param.audioCodec.flac"),
                Language.get("param.audioCodec.opus")
        );
        String currentCodec = (String) opts.getOrDefault("audioCodec", Language.get("param.audioCodec.mp3"));
        aCodec.setValue(currentCodec);
        aCodec.valueProperty().addListener((obs, old, val) -> {
            task.setOption("audioCodec", val);
            if (onParamChanged != null) onParamChanged.run();
        });
        grid.add(aCodec, 1, row++);

        Label bitrateLabel = new Label(Language.get("param.bitrate"));
        grid.add(bitrateLabel, 0, row);
        ComboBox<String> bitrate = new ComboBox<>();
        bitrate.getItems().addAll("64k", "128k", "192k", "320k");
        bitrate.setValue((String) opts.getOrDefault("audioBitrate", "128k"));
        bitrate.valueProperty().addListener((obs, old, val) -> {
            task.setOption("audioBitrate", val);
            if (onParamChanged != null) onParamChanged.run();
        });
        grid.add(bitrate, 1, row++);
        return grid;
    }

    private static GridPane createImageGrid(ConversionTask task, Map<String, Object> opts, Runnable onParamChanged, ComboBox<String> formatCombo) {
        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(10);
        grid.setPadding(new Insets(5, 0, 5, 0));
        int row = 0;

        String currentFormat = task.getOutputFormat().toLowerCase();
        boolean showQuality = currentFormat.matches("jpeg|jpg|webp");

        if (showQuality) {
            Label qualityLabel = new Label(Language.get("param.imageQuality"));
            grid.add(qualityLabel, 0, row);
            int currentQuality = ((Number) opts.getOrDefault("imageQuality", 90)).intValue();
            Slider slider = new Slider(1, 100, currentQuality);
            slider.setShowTickLabels(true);
            Label qValLabel = new Label(currentQuality + "%");
            slider.valueProperty().addListener((obs, old, val) -> {
                task.setOption("imageQuality", val.intValue());
                qValLabel.setText(val.intValue() + "%");
                if (onParamChanged != null) onParamChanged.run();
            });
            HBox qualityBox = new HBox(8, slider, qValLabel);
            grid.add(qualityBox, 1, row++);
        } else {
            Label note = new Label(Language.get("param.image.lossless"));
            note.setWrapText(true);
            note.getStyleClass().add("label-muted");
            grid.add(note, 0, row++, 2, 1);
        }

        Label scaleLabel = new Label(Language.get("param.scaleMode"));
        grid.add(scaleLabel, 0, row);
        ComboBox<String> scaleMode = new ComboBox<>();
        scaleMode.getItems().addAll(Language.get("param.scale.keep"), Language.get("param.scale.custom"));
        scaleMode.setValue((String) opts.getOrDefault("scaleMode", Language.get("param.scale.keep")));
        grid.add(scaleMode, 1, row++);

        HBox scaleBox = new HBox(4);
        TextField scaleWidth = new TextField(); scaleWidth.setPromptText(Language.get("param.width"));
        TextField scaleHeight = new TextField(); scaleHeight.setPromptText(Language.get("param.height"));
        scaleWidth.setPrefWidth(55);
        scaleHeight.setPrefWidth(55);
        scaleBox.getChildren().addAll(scaleWidth, new Label("×"), scaleHeight);
        scaleBox.setVisible(Language.get("param.scale.custom").equals(scaleMode.getValue()));
        grid.add(scaleBox, 1, row++);

        scaleMode.valueProperty().addListener((obs, old, val) -> {
            scaleBox.setVisible(Language.get("param.scale.custom").equals(val));
            task.setOption("scaleMode", val);
            if (onParamChanged != null) onParamChanged.run();
        });

        Runnable updateCustomSize = () -> {
            if (Language.get("param.scale.custom").equals(scaleMode.getValue()) && !scaleWidth.getText().isEmpty() && !scaleHeight.getText().isEmpty()) {
                task.setOption("customSize", scaleWidth.getText() + "x" + scaleHeight.getText());
                if (onParamChanged != null) onParamChanged.run();
            }
        };
        scaleWidth.textProperty().addListener((obs, old, val) -> updateCustomSize.run());
        scaleHeight.textProperty().addListener((obs, old, val) -> updateCustomSize.run());
        return grid;
    }
}