package net.alan.senlo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FFmpegChecker {
    private static final Logger logger = LoggerFactory.getLogger(FFmpegChecker.class);
    private static String ffmpegPath = null;

    static {
        detectFFmpeg();
    }

    public static List<String> getAvailableHardwareEncoders() {
        List<String> available = new ArrayList<>();
        String ffmpegPath = getFFmpegPath();
        if (ffmpegPath == null) return available;

        try {
            Process process = new ProcessBuilder(ffmpegPath, "-encoders").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("h264_") && (line.contains("nvenc") || line.contains("qsv") || line.contains("amf") || line.contains("videotoolbox"))) {
                        String enc = line.substring(line.lastIndexOf(" ")).trim();
                        available.add(enc);
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            logger.error("检测硬件编码器失败", e);
        }
        return available;
    }

    private static void detectFFmpeg() {
        // 方法1：通过 JAVE2 的 DefaultFFMPEGLocator 获取内置 FFmpeg 路径（新版推荐）
        try {
            DefaultFFMPEGLocator locator = new DefaultFFMPEGLocator();
            // 🔧 修复点：改用新版API getExecutablePath()
            String path = locator.getExecutablePath();
            if (path != null && new File(path).exists()) {
                ffmpegPath = path;
                logger.info("JAVE2 内置 FFmpeg 可用: {}", ffmpegPath);
                return;
            }
        } catch (Exception e) {
            logger.warn("JAVE2 获取 FFmpeg 路径失败", e);
        }

        // 方法2：从系统 PATH 查找
        String[] candidates = {"ffmpeg", "ffmpeg.exe"};
        for (String cmd : candidates) {
            try {
                Process process = new ProcessBuilder(cmd, "-version").start();
                if (process.waitFor() == 0) {
                    ffmpegPath = cmd;
                    logger.info("从系统 PATH 中找到 FFmpeg: {}", ffmpegPath);
                    return;
                }
            } catch (Exception ignored) {}
        }

        // 方法3：常见安装路径（Windows）
        String[] commonPaths = {
                "C:\\ffmpeg\\bin\\ffmpeg.exe",
                "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
                System.getProperty("user.home") + "\\ffmpeg\\bin\\ffmpeg.exe"
        };
        for (String path : commonPaths) {
            if (new File(path).exists()) {
                ffmpegPath = path;
                logger.info("在常见路径中找到 FFmpeg: {}", ffmpegPath);
                return;
            }
        }

        // 方法4：尝试查找临时目录下 JAVE2 可能解压的文件
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            File javeDir = new File(tmpDir, "jave");
            if (javeDir.exists() && javeDir.isDirectory()) {
                File[] files = javeDir.listFiles((dir, name) -> name.contains("ffmpeg") && name.endsWith(".exe"));
                if (files != null && files.length > 0) {
                    ffmpegPath = files[0].getAbsolutePath();
                    logger.info("在临时目录中找到 FFmpeg: {}", ffmpegPath);
                    return;
                }
            }
        } catch (Exception e) {
            logger.warn("搜索临时目录失败", e);
        }

        logger.error("未找到 FFmpeg，请确保 JAVE2 依赖完整或网络正常（首次运行会自动下载）");
        ffmpegPath = null;
    }

    public static String getFFmpegPath() {
        return ffmpegPath;
    }

    public static boolean isAvailable() {
        return ffmpegPath != null;
    }

}