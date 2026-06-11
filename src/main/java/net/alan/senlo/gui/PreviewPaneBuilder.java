package net.alan.senlo.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import net.alan.senlo.model.ConversionTask;
import net.alan.senlo.util.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;

public class PreviewPaneBuilder {
    private static final Logger logger = LoggerFactory.getLogger(PreviewPaneBuilder.class);
    private static MediaPlayer currentMediaPlayer = null;

    private static final double PREVIEW_WIDTH = 400;
    private static final double PREVIEW_HEIGHT = 300;

    public static void releaseCurrentMedia() {
        if (currentMediaPlayer != null) {
            try {
                currentMediaPlayer.stop();
                currentMediaPlayer.dispose();
            } catch (Exception ignored) {}
            currentMediaPlayer = null;
        }
    }

    public static VBox buildPreviewPane(ConversionTask task) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8));
        box.getStyleClass().add("preview-pane");

        if (task == null) {
            Label label = new Label(Language.get("preview.noSelection"));
            label.getStyleClass().add("label-muted");
            box.getChildren().add(label);
            return box;
        }

        File file = new File(task.getInputPath());
        if (!file.exists()) {
            Label label = new Label(Language.get("preview.fileNotFound") + task.getFileName());
            label.setStyle("-fx-text-fill: #A80000;");
            box.getChildren().add(label);
            return box;
        }

        String mediaType = task.getMediaType();
        String name = task.getFileName();

        try {
            switch (mediaType) {
                case "image":
                    addImagePreview(box, file);
                    break;
                case "video":
                    addVideoPreview(box, file);
                    break;
                case "audio":
                    addAudioPreview(box, file);
                    break;
                case "document":
                    addDocumentPreview(box, file);
                    break;
                default:
                    Label label = new Label(Language.get("preview.unsupported") + name);
                    label.getStyleClass().add("label-muted");
                    box.getChildren().add(label);
            }
        } catch (Exception e) {
            logger.error("预览构建失败", e);
            Label label = new Label(Language.get("preview.loadFailed") + e.getMessage());
            label.setStyle("-fx-text-fill: #A80000;");
            box.getChildren().add(label);
        }

        return box;
    }

    private static void addImagePreview(VBox box, File file) {
        try {
            Image image = new Image(file.toURI().toString(), PREVIEW_WIDTH, PREVIEW_HEIGHT, true, true);
            ImageView iv = new ImageView(image);
            iv.setPreserveRatio(true);
            iv.setFitWidth(PREVIEW_WIDTH);
            iv.setFitHeight(PREVIEW_HEIGHT);

            HBox centerBox = new HBox(iv);
            centerBox.setAlignment(Pos.CENTER);
            box.getChildren().add(centerBox);
        } catch (Exception e) {
            box.getChildren().add(new Label(Language.get("preview.imageLoadFailed") + e.getMessage()));
        }
    }

    private static void addVideoPreview(VBox box, File file) {
        String path = file.toURI().toString();
        String ext = getExtension(file).toLowerCase();

        if (!isVideoSupported(ext)) {
            Label label = new Label(Language.get("preview.videoUnsupported"));
            label.setWrapText(true);
            label.getStyleClass().add("label-muted");
            box.getChildren().add(label);
            return;
        }

        try {
            Media media = new Media(path);
            MediaPlayer player = new MediaPlayer(media);
            releaseCurrentMedia();
            currentMediaPlayer = player;

            MediaView mediaView = new MediaView(player);
            mediaView.setFitWidth(PREVIEW_WIDTH);
            mediaView.setFitHeight(PREVIEW_HEIGHT);
            mediaView.setPreserveRatio(true);

            HBox centerBox = new HBox(mediaView);
            centerBox.setAlignment(Pos.CENTER);

            HBox controls = createMediaControls(player);
            player.setCycleCount(1);
            VBox.setVgrow(centerBox, Priority.ALWAYS);
            box.getChildren().addAll(centerBox, controls);
        } catch (Exception e) {
            logger.warn("视频预览失败: {}", e.getMessage());
            box.getChildren().add(new Label(Language.get("preview.videoFailed") + e.getMessage()));
        }
    }

    private static void addAudioPreview(VBox box, File file) {
        String path = file.toURI().toString();
        String ext = getExtension(file).toLowerCase();
        if (!isAudioSupported(ext)) {
            Label label = new Label(Language.get("preview.audioUnsupported"));
            label.setWrapText(true);
            label.getStyleClass().add("label-muted");
            box.getChildren().add(label);
            return;
        }

        try {
            Media media = new Media(path);
            MediaPlayer player = new MediaPlayer(media);
            releaseCurrentMedia();
            currentMediaPlayer = player;

            Label audioLabel = new Label(Language.get("preview.playingAudio") + file.getName());
            audioLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E0E0E0;");
            HBox controls = createMediaControls(player);
            box.getChildren().addAll(audioLabel, controls);
        } catch (Exception e) {
            logger.warn("音频预览失败: {}", e.getMessage());
            box.getChildren().add(new Label(Language.get("preview.audioFailed") + e.getMessage()));
        }
    }

    private static void addDocumentPreview(VBox box, File file) {
        String ext = getExtension(file).toLowerCase();
        if (ext.matches("txt|md|java|xml|json|properties|log")) {
            try {
                String content = Files.readString(file.toPath());
                TextArea textArea = new TextArea(content);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setPrefRowCount(10);
                textArea.setMaxHeight(Double.MAX_VALUE);
                textArea.getStyleClass().add("text-preview");
                VBox.setVgrow(textArea, Priority.ALWAYS);
                box.getChildren().add(textArea);
            } catch (Exception e) {
                box.getChildren().add(new Label(Language.get("preview.textFailed") + e.getMessage()));
            }
        } else if (ext.equals("pdf")) {
            Label label = new Label(Language.get("preview.pdfHint"));
            label.setWrapText(true);
            label.getStyleClass().add("label-muted");
            box.getChildren().add(label);
        } else if (ext.matches("html|htm")) {
            Label label = new Label(Language.get("preview.htmlHint"));
            label.setWrapText(true);
            label.getStyleClass().add("label-muted");
            box.getChildren().add(label);
        } else {
            box.getChildren().add(new Label(Language.get("preview.documentUnsupported")));
        }
    }

    private static HBox createMediaControls(MediaPlayer player) {
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(5));
        controls.setAlignment(Pos.CENTER);

        Button playPauseBtn = new Button("▶");
        playPauseBtn.getStyleClass().add("media-button");

        Slider timeSlider = new Slider();
        timeSlider.setPrefWidth(300);
        timeSlider.getStyleClass().add("media-slider");

        Label timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: #E0E0E0;");

        timeSlider.setDisable(true);
        timeLabel.setText("--:-- / --:--");

        player.setOnReady(() -> {
            Duration total = player.getTotalDuration();
            if (total != null && !total.isUnknown() && total.toSeconds() > 0) {
                timeSlider.setDisable(false);
                timeSlider.setMax(1.0);
                timeLabel.setText("00:00 / " + formatTime(total.toSeconds()));
            } else {
                timeSlider.setDisable(true);
                timeLabel.setText("00:00 / ??:??");
            }
        });

        playPauseBtn.setOnAction(e -> {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) {
                player.pause();
                playPauseBtn.setText("▶");
            } else {
                player.play();
                playPauseBtn.setText("⏸");
            }
        });

        player.currentTimeProperty().addListener((obs, old, now) -> {
            if (!timeSlider.isValueChanging() && !timeSlider.isDisabled()) {
                Duration total = player.getTotalDuration();
                if (total != null && total.toSeconds() > 0) {
                    double progress = now.toSeconds() / total.toSeconds();
                    timeSlider.setValue(progress);
                    timeLabel.setText(formatTime(now.toSeconds()) + " / " + formatTime(total.toSeconds()));
                }
            }
        });

        timeSlider.valueProperty().addListener((obs, old, val) -> {
            if (timeSlider.isValueChanging() && !timeSlider.isDisabled()) {
                Duration total = player.getTotalDuration();
                if (total != null && total.toSeconds() > 0) {
                    double targetSeconds = val.doubleValue() * total.toSeconds();
                    player.seek(Duration.seconds(targetSeconds));
                }
            }
        });

        player.setOnEndOfMedia(() -> {
            playPauseBtn.setText("▶");
            timeSlider.setValue(0);
        });

        player.setOnError(() -> {
            String errorMsg = player.getError() != null ? player.getError().getMessage() : "未知错误";
            logger.warn("媒体播放错误: {}", errorMsg);
            timeSlider.setDisable(true);
            timeLabel.setText(Language.get("preview.playError"));
            playPauseBtn.setDisable(true);
        });

        controls.getChildren().addAll(playPauseBtn, timeSlider, timeLabel);
        return controls;
    }

    private static String formatTime(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds < 0) return "00:00";
        int totalSec = (int) seconds;
        int mins = totalSec / 60;
        int secs = totalSec % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private static String getExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return (dot == -1) ? "" : name.substring(dot + 1);
    }

    private static boolean isVideoSupported(String ext) {
        return ext.matches("mp4|m4v|mov|flv|avi|mkv|webm|3gp|3g2|wmv|mpg|mpeg");
    }

    private static boolean isAudioSupported(String ext) {
        return ext.matches("mp3|wav|m4a|aac|flac|ogg|opus");
    }
}