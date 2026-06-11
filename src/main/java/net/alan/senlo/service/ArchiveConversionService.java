// 文件路径：src/main/java/net/alan/senlo/service/ArchiveConversionService.java
package net.alan.senlo.service;

import net.alan.senlo.model.ConversionTask;
import net.alan.senlo.util.SevenZipHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class ArchiveConversionService {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveConversionService.class);

    /**
     * 转换压缩包
     * @param task       转换任务
     * @param onProgress 进度回调（目前7z输出粒度较粗，可先不实现精确进度）
     * @return 是否成功
     */
    public boolean convert(ConversionTask task, Consumer<Double> onProgress) {
        String srcPath = task.getInputPath();
        String dstPath = task.getOutputPath();
        String targetFormat = task.getOutputFormat().toLowerCase(); // zip, 7z, tar 等

        Path tempDir = null;
        try {
            // 1. 创建临时工作目录
            tempDir = Files.createTempDirectory("senlo_archive_");
            Path extractedDir = tempDir.resolve("extracted");
            Files.createDirectories(extractedDir);

            // 2. 解压源文件
            logger.info("开始解压: {} -> {}", srcPath, extractedDir);
            if (!extractArchive(srcPath, extractedDir, onProgress)) {
                return false;
            }

            // 3. 重新打包为目标格式
            logger.info("开始打包: {} -> {}", extractedDir, dstPath);
            return packArchive(extractedDir, dstPath, targetFormat, onProgress);

        } catch (Exception e) {
            logger.error("压缩包转换失败", e);
            return false;
        } finally {
            // 4. 清理临时目录
            if (tempDir != null) {
                deleteDirectory(tempDir.toFile());
            }
        }
    }

    private boolean extractArchive(String srcPath, Path destDir, Consumer<Double> onProgress) throws Exception {
        File sevenZip = SevenZipHelper.getSevenZip();
        ProcessBuilder pb = new ProcessBuilder(
                sevenZip.getAbsolutePath(),
                "x", srcPath,
                "-o" + destDir.toString(),
                "-y"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        // 读取输出（可记录日志，也可尝试解析进度）
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("7z extract: {}", line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("解压失败，退出码: {}", exitCode);
            return false;
        }
        // 解压完成更新一次进度
        onProgress.accept(0.5);
        return true;
    }

    private boolean packArchive(Path sourceDir, String dstPath, String format, Consumer<Double> onProgress) throws Exception {
        File sevenZip = SevenZipHelper.getSevenZip();
        String formatArg = format;
        if ("zip".equals(formatArg)) {
            formatArg = "zip";
        } else if ("7z".equals(formatArg)) {
            formatArg = "7z";
        } else if ("tar".equals(formatArg)) {
            formatArg = "tar";
        } else {
            formatArg = "zip";
        }

        // 关键修改：在目录路径后加上 "\*" 或 "/*"（跨平台用 File.separator + "*"）
        String sourceDirPath = sourceDir.toString() + File.separator + "*";

        ProcessBuilder pb = new ProcessBuilder(
                sevenZip.getAbsolutePath(),
                "a", dstPath,
                sourceDirPath,
                "-t" + formatArg,
                "-y"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("7z pack: {}", line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("打包失败，退出码: {}", exitCode);
            return false;
        }
        onProgress.accept(1.0);
        return true;
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }
}