package net.alan.senlo.model;

import javafx.beans.property.*;
import net.alan.senlo.util.Language;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConversionTask {
    private final StringProperty fileName;
    private final StringProperty outputFormat;
    private final DoubleProperty progress;
    private final StringProperty status;
    private final String inputPath;
    private String outputPath;
    private Process ffmpegProcess;

    private final BooleanProperty selected;
    private final StringProperty paramSummary;
    private Map<String, Object> options;
    private String mediaType;

    public ConversionTask(String inputPath) {
        this.inputPath = inputPath;
        this.fileName = new SimpleStringProperty(new File(inputPath).getName());
        this.outputFormat = new SimpleStringProperty("MP4");
        this.progress = new SimpleDoubleProperty(0);
        this.status = new SimpleStringProperty(Language.get("status.waiting"));
        this.selected = new SimpleBooleanProperty(false);
        this.paramSummary = new SimpleStringProperty("");
        this.options = new HashMap<>();
        this.mediaType = detectMediaType(inputPath);
        updateParamSummary();
    }

    private String detectMediaType(String path) {
        String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
        if (ext.matches("mp4|mkv|avi|mov|wmv|flv|webm|3gp|vob|ts|rmvb|mpg|mpeg")) return "video";
        if (ext.matches("mp3|wma|aac|m4a|ogg|opus|flac|alac|ape|wav|aiff|amr|ac3|dts")) return "audio";
        if (ext.matches("bmp|gif|jpg|jpeg|jfif|png|tiff|webp|avif|heic|apng|psd|svg|ico|dng|raw|eps|pcx|cdr|ufo")) return "image";
        if (ext.matches("docx?|xlsx?|pptx?|pdf|odt|ods|odp|txt|rtf|html?|md|epub|mobi|chm")) return "document";
        if (ext.matches("zip|7z|rar|tar|gz|bz2|jar")) return "archive";
        return "unknown";
    }

    public void updateParamSummary() {
        StringBuilder sb = new StringBuilder();
        if ("video".equals(mediaType)) {
            String resolution = (String) options.getOrDefault("resolution", Language.get("param.resolution.original"));
            String videoCodec = (String) options.getOrDefault("videoCodec", Language.get("param.videoCodec.h264"));
            String audioCodec = (String) options.getOrDefault("audioCodec", Language.get("param.audioCodec.aac"));
            sb.append(resolution).append(" ").append(videoCodec).append("/").append(audioCodec);
        } else if ("audio".equals(mediaType)) {
            String codec = (String) options.getOrDefault("audioCodec", Language.get("param.audioCodec.mp3"));
            String bitrate = options.getOrDefault("audioBitrate", "128k").toString();
            sb.append(codec).append(" ").append(bitrate);
        } else if ("image".equals(mediaType)) {
            String quality = options.getOrDefault("imageQuality", "90%").toString();
            sb.append(Language.get("param.image.qualityPrefix")).append(" ").append(quality);
        } else {
            sb.append(Language.get("param.defaultSummary"));
        }
        paramSummary.set(sb.toString());
    }

    public boolean isSelected() { return selected.get(); }
    public BooleanProperty selectedProperty() { return selected; }
    public void setSelected(boolean selected) { this.selected.set(selected); }

    public String getParamSummary() { return paramSummary.get(); }
    public StringProperty paramSummaryProperty() { return paramSummary; }
    public void setParamSummary(String summary) { paramSummary.set(summary); }

    public Map<String, Object> getOptions() { return options; }
    public void setOptions(Map<String, Object> options) {
        this.options = options;
        updateParamSummary();
    }
    public void setOption(String key, Object value) {
        this.options.put(key, value);
        updateParamSummary();
    }

    public String getMediaType() { return mediaType; }
    public StringProperty fileNameProperty() { return fileName; }
    public StringProperty outputFormatProperty() { return outputFormat; }
    public DoubleProperty progressProperty() { return progress; }
    public StringProperty statusProperty() { return status; }
    public String getFileName() { return fileName.get(); }
    public String getOutputFormat() { return outputFormat.get(); }
    public double getProgress() { return progress.get(); }
    public String getStatus() { return status.get(); }
    public String getInputPath() { return inputPath; }
    public String getOutputPath() { return outputPath; }
    public void setFileName(String name) { fileName.set(name); }
    public void setOutputFormat(String format) { outputFormat.set(format); }
    public void setProgress(double p) { progress.set(p); }
    public void setStatus(String s) { status.set(s); }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    public Process getFfmpegProcess() { return ffmpegProcess; }
    public void setFfmpegProcess(Process ffmpegProcess) { this.ffmpegProcess = ffmpegProcess; }
}