package net.alan.senlo.service;

import javafx.application.Platform;
import net.alan.senlo.model.ApplicationSettings;
import net.alan.senlo.model.ConversionTask;
import net.alan.senlo.model.FormatRegistry;
import net.alan.senlo.util.FFmpegChecker;
import net.alan.senlo.util.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Jave2ConversionService {
    private static final Logger logger = LoggerFactory.getLogger(Jave2ConversionService.class);
    private final Map<ConversionTask, Process> runningProcesses = new ConcurrentHashMap<>();
    private final ArchiveConversionService archiveService = new ArchiveConversionService();

    private long getDurationUs(File source) {
        String ffmpegPath = FFmpegChecker.getFFmpegPath();
        if (ffmpegPath == null) return -1;
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-i", source.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Duration:")) {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Duration: (\\d{2}):(\\d{2}):(\\d{2}\\.?\\d*)").matcher(line);
                        if (m.find()) {
                            int hours = Integer.parseInt(m.group(1));
                            int minutes = Integer.parseInt(m.group(2));
                            double seconds = Double.parseDouble(m.group(3));
                            double totalSec = hours * 3600 + minutes * 60 + seconds;
                            return (long) (totalSec * 1_000_000);
                        }
                    }
                }
            }
            p.waitFor();
        } catch (Exception e) {
            logger.error("获取媒体时长失败", e);
        }
        return -1;
    }

    public boolean convert(ConversionTask task, Consumer<Double> onProgress) {
        String mediaType = task.getMediaType();
        String outputFormat = task.getOutputFormat().toLowerCase();

        if ("archive".equals(mediaType)) {
            return archiveService.convert(task, onProgress);
        }

        if (outputFormat.equals("gif动画")) outputFormat = "gif";
        boolean targetIsAudio = outputFormat.matches("mp3|aac|m4a|wav|flac|opus|ogg");
        boolean targetIsImage = outputFormat.matches("bmp|gif|jpg|jpeg|png|webp|ico|tiff");

        if ("document".equals(mediaType)) {
            Platform.runLater(() -> {
                task.setStatus(Language.get("status.unsupported"));
                task.setProgress(0);
            });
            logger.warn("任务 {} 类型为 document，不支持通过 FFmpeg 转换", task.getFileName());
            return false;
        }

        if ("audio".equals(mediaType) && targetIsImage) {
            Platform.runLater(() -> task.setStatus(Language.get("status.audioToImageNotSupported")));
            return false;
        }

        String ffmpegPath = FFmpegChecker.getFFmpegPath();
        if (ffmpegPath == null || ffmpegPath.isBlank()) {
            Platform.runLater(() -> task.setStatus(Language.get("status.ffmpegNotFound")));
            return false;
        }

        String outputPath = task.getOutputPath();
        if (outputPath == null) return false;

        File source = new File(task.getInputPath());
        File target = new File(outputPath);
        target.getParentFile().mkdirs();

        long totalDurationUs = getDurationUs(source);
        if (totalDurationUs <= 0) {
            logger.warn("无法获取源文件时长，进度将无法显示");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath);
        cmd.add("-i");
        cmd.add(source.getAbsolutePath());
        cmd.add("-y");
        buildFFmpegParams(task, cmd, outputFormat);
        cmd.add(target.getAbsolutePath());
        cmd.add("-progress");
        cmd.add("-");
        cmd.add("-hide_banner");

        logger.info("执行命令: {}", String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            runningProcesses.put(task, process);
            task.setFfmpegProcess(process);

            long[] lastUpdateMs = {System.currentTimeMillis()};
            double[] lastProgress = {0.0};
            StringBuilder errorOutput = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("FFmpeg: {}", line);
                    if (line.startsWith("out_time_ms=") && totalDurationUs > 0) {
                        String timeStr = line.substring("out_time_ms=".length());
                        try {
                            long currentUs = Long.parseLong(timeStr);
                            double progress = Math.min(1.0, (double) currentUs / totalDurationUs);
                            long now = System.currentTimeMillis();
                            if (now - lastUpdateMs[0] >= 200 || progress >= 1.0) {
                                if (Math.abs(progress - lastProgress[0]) > 0.01) {
                                    final double p = progress;
                                    Platform.runLater(() -> {
                                        onProgress.accept(p);
                                        task.setProgress(p);
                                    });
                                    lastProgress[0] = progress;
                                }
                                lastUpdateMs[0] = now;
                            }
                        } catch (NumberFormatException ignored) {}
                    } else if (line.startsWith("frame=") && totalDurationUs <= 0) {
                        String frameStr = line.substring("frame=".length()).trim().split(" ")[0];
                        if (frameStr.matches("\\d+")) {
                            int frame = Integer.parseInt(frameStr);
                            double progress = Math.min(0.99, frame / 100.0);
                            if (Math.abs(progress - lastProgress[0]) > 0.02) {
                                final double p = progress;
                                Platform.runLater(() -> {
                                    onProgress.accept(p);
                                    task.setProgress(p);
                                });
                                lastProgress[0] = progress;
                            }
                        }
                    } else if (line.toLowerCase().contains("error")) {
                        errorOutput.append(line).append("\n");
                    }
                }
            }

            int exitCode = process.waitFor();
            boolean success = exitCode == 0;
            Platform.runLater(() -> {
                if (success) {
                    task.setStatus(Language.get("status.completed"));
                    task.setProgress(1.0);
                    onProgress.accept(1.0);
                } else {
                    String errMsg = errorOutput.length() > 0 ? errorOutput.toString() : Language.get("status.conversionFailed");
                    task.setStatus(Language.get("status.failed"));
                    logger.error("FFmpeg 转换失败，错误输出: {}", errMsg);
                }
            });
            return success;
        } catch (Exception e) {
            logger.error("转换异常", e);
            Platform.runLater(() -> task.setStatus(Language.get("status.failed") + ": " + e.getMessage()));
            return false;
        } finally {
            runningProcesses.remove(task);
        }
    }

    public void stopTask(ConversionTask task) {
        Process process = runningProcesses.get(task);
        if (process != null && process.isAlive()) {
            process.destroy();
            logger.info("已强制停止任务: {}", task.getFileName());
        }
    }

    private void buildFFmpegParams(ConversionTask task, List<String> cmd, String outputFormat) {
        Map<String, Object> opts = task.getOptions();
        String mediaType = task.getMediaType();

        boolean targetIsAudio = outputFormat.matches("mp3|aac|m4a|wav|flac|opus|ogg");
        boolean targetIsImage = outputFormat.matches("bmp|gif|jpg|jpeg|png|webp|ico|tiff");
        boolean isGifAnimation = outputFormat.equals("gif") && "video".equals(mediaType);

        if (targetIsAudio) {
            cmd.add("-vn");
            String acodec;
            switch (outputFormat) {
                case "mp3": acodec = "libmp3lame"; break;
                case "aac": acodec = "aac"; break;
                case "m4a": acodec = "aac"; break;
                case "wav": acodec = "pcm_s16le"; break;
                case "flac": acodec = "flac"; break;
                case "opus": acodec = "libopus"; break;
                case "ogg": acodec = "libvorbis"; break;
                default: acodec = "copy";
            }
            cmd.add("-c:a");
            cmd.add(acodec);
            String bitrate = (String) opts.get("audioBitrate");
            if (bitrate != null && !bitrate.isEmpty()) {
                cmd.add("-b:a");
                cmd.add(bitrate);
            }
            if (!outputFormat.equals("m4a")) {
                cmd.add("-f");
                cmd.add(outputFormat);
            } else {
                cmd.add("-f");
                cmd.add("mp4");
            }
        } else if (targetIsImage) {
            if (isGifAnimation) {
                cmd.add("-c:v"); cmd.add("gif");
                double fps = ((Number) opts.getOrDefault("gifFps", 10.0)).doubleValue();
                String scaleFilter = "";
                String scaleMode = (String) opts.getOrDefault("scaleMode", Language.get("param.scale.keep"));
                if (Language.get("param.scale.custom").equals(scaleMode) && opts.containsKey("customSize")) {
                    String size = (String) opts.get("customSize");
                    if (size != null && size.matches("\\d+x\\d+")) {
                        scaleFilter = ",scale=" + size;
                    }
                }
                String filterComplex = String.format("fps=%f%s,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse", fps, scaleFilter);
                cmd.add("-vf"); cmd.add(filterComplex);
                cmd.add("-an");
            } else {
                String vcodec;
                switch (outputFormat) {
                    case "bmp": vcodec = "bmp"; break;
                    case "gif": vcodec = "gif"; break;
                    case "jpg": case "jpeg": vcodec = "mjpeg"; break;
                    case "png": vcodec = "png"; break;
                    case "webp": vcodec = "libwebp"; break;
                    case "ico": vcodec = "png"; break;
                    default: vcodec = "copy";
                }
                cmd.add("-c:v");
                cmd.add(vcodec);
                cmd.add("-frames:v");
                cmd.add("1");
                if (opts.containsKey("imageQuality") && (outputFormat.equals("jpg") || outputFormat.equals("jpeg") || outputFormat.equals("webp"))) {
                    int quality = ((Number) opts.get("imageQuality")).intValue();
                    cmd.add("-q:v");
                    cmd.add(String.valueOf(quality));
                }
                String scaleMode = (String) opts.getOrDefault("scaleMode", Language.get("param.scale.keep"));
                if (Language.get("param.scale.custom").equals(scaleMode) && opts.containsKey("customSize")) {
                    String size = (String) opts.get("customSize");
                    if (size != null && size.matches("\\d+x\\d+")) {
                        cmd.add("-vf");
                        cmd.add("scale=" + size);
                    }
                } else if (outputFormat.equals("ico")) {
                    cmd.add("-vf");
                    cmd.add("scale=256:256");
                }
                cmd.add("-an");
            }
        } else {
            if ("video".equals(mediaType)) {
                String vcodec = (String) opts.getOrDefault("videoCodec", Language.get("param.videoCodec.h264"));
                cmd.add("-c:v");
                switch (vcodec) {
                    case "H.264": cmd.add("libx264"); cmd.add("-preset"); cmd.add("veryfast"); break;
                    case "H.265/HEVC": cmd.add("libx265"); cmd.add("-preset"); cmd.add("veryfast"); break;
                    case "VP9": cmd.add("libvpx-vp9"); break;
                    case "Copy": cmd.add("copy"); break;
                    default: cmd.add("libx264"); cmd.add("-preset"); cmd.add("veryfast");
                }
                if (opts.containsKey("crf") && !"Copy".equals(vcodec)) {
                    cmd.add("-crf");
                    cmd.add(String.valueOf(((Number) opts.get("crf")).intValue()));
                }
                String resolution = (String) opts.getOrDefault("resolution", Language.get("param.resolution.original"));
                if (!Language.get("param.resolution.original").equals(resolution) && resolution.matches("\\d+x\\d+")) {
                    cmd.add("-vf");
                    cmd.add("scale=" + resolution);
                }
                String acodec = (String) opts.getOrDefault("audioCodec", Language.get("param.audioCodec.aac"));
                cmd.add("-c:a");
                switch (acodec) {
                    case "AAC": cmd.add("aac"); break;
                    case "MP3": cmd.add("libmp3lame"); break;
                    case "Opus": cmd.add("libopus"); break;
                    case "FLAC": cmd.add("flac"); break;
                    default: cmd.add("aac");
                }
                String abitrate = (String) opts.get("audioBitrate");
                if (abitrate != null && !abitrate.isEmpty()) {
                    cmd.add("-b:a");
                    cmd.add(abitrate);
                }
            } else if ("audio".equals(mediaType)) {
                cmd.add("-vn");
                String acodec;
                switch (outputFormat) {
                    case "mp3": acodec = "libmp3lame"; break;
                    case "aac": acodec = "aac"; break;
                    case "flac": acodec = "flac"; break;
                    case "opus": acodec = "libopus"; break;
                    case "ogg": acodec = "libvorbis"; break;
                    default: acodec = "copy";
                }
                cmd.add("-c:a");
                cmd.add(acodec);
                String bitrate = (String) opts.get("audioBitrate");
                if (bitrate != null && !bitrate.isEmpty()) {
                    cmd.add("-b:a");
                    cmd.add(bitrate);
                }
                cmd.add("-f");
                cmd.add(outputFormat);
            } else {
                logger.warn("未知媒体类型: {}，使用流拷贝模式", mediaType);
                cmd.add("-c");
                cmd.add("copy");
            }
        }
    }

    public String buildOutputPath(ConversionTask task) {
        File inputFile = new File(task.getInputPath());
        String name = inputFile.getName();
        int dot = name.lastIndexOf('.');
        String base = (dot == -1) ? name : name.substring(0, dot);
        String outputDir = ApplicationSettings.getInstance().getOutputDirectory();
        String formatName = task.getOutputFormat();

        if ("archive".equals(task.getMediaType())) {
            FormatRegistry.FormatInfo info = FormatRegistry.getByName(formatName);
            String ext;
            if (info != null) {
                ext = info.getPrimaryExtension();
                if (ext.startsWith(".")) ext = ext.substring(1);
            } else {
                ext = formatName.toLowerCase();
            }
            return new File(outputDir, base + "." + ext).getAbsolutePath();
        }

        if ("GIF动画".equals(formatName)) formatName = "gif";
        FormatRegistry.FormatInfo info = FormatRegistry.getByName(formatName);
        String ext;
        if (info != null) {
            ext = info.getPrimaryExtension();
            if (ext.startsWith(".")) ext = ext.substring(1);
        } else {
            ext = formatName.toLowerCase();
        }
        if ("matroska".equals(ext)) ext = "mkv";
        if ("adts".equals(ext)) ext = "aac";
        if ("mjpeg".equals(ext)) ext = "jpg";
        return new File(outputDir, base + "." + ext).getAbsolutePath();
    }
}