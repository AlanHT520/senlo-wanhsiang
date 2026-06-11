package net.alan.senlo.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    public static String getExtension(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex == -1) ? "" : name.substring(dotIndex + 1).toLowerCase();
    }

    public static String getExtension(String path) {
        return getExtension(new File(path));
    }

    public static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static String buildOutputPath(String inputPath, String outputDir, String outputExt) {
        File inputFile = new File(inputPath);
        String baseName = inputFile.getName();
        int dotIndex = baseName.lastIndexOf('.');
        String nameWithoutExt = (dotIndex == -1) ? baseName : baseName.substring(0, dotIndex);
        return Paths.get(outputDir, nameWithoutExt + "." + outputExt).toString();
    }
}