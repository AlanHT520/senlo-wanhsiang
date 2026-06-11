package net.alan.senlo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.Files;

public final class SevenZipHelper {
    private static final Logger logger = LoggerFactory.getLogger(SevenZipHelper.class);
    private static File sevenZipExecutable;

    public static synchronized File getSevenZip() throws IOException {
        if (sevenZipExecutable != null && sevenZipExecutable.exists()) {
            return sevenZipExecutable;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        if (!os.contains("win") || !arch.contains("64")) {
            throw new UnsupportedOperationException("压缩包转换目前仅支持 Windows 64 位系统");
        }

        // 只加载 7za.exe
        String resourcePath = "/bin/7za.exe";
        try (InputStream in = SevenZipHelper.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("内置 7za.exe 缺失，请确认 resources/bin/ 下存在 7za.exe");
            }
            File tempFile = File.createTempFile("7za_", ".exe");
            tempFile.deleteOnExit();
            Files.copy(in, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            sevenZipExecutable = tempFile;
            logger.info("7za 已准备就绪: {}", sevenZipExecutable.getAbsolutePath());
            return sevenZipExecutable;
        }
    }

    private SevenZipHelper() {}
}