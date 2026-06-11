package net.alan.senlo.config;

import java.io.File;

public final class Config {
    public static final String SOFTWARE_NAME = "森罗万象 · Senlo-Wanhsiang";
    public static final String VERSION = "0.1.0";
    public static final String CH = "Alpha";
    public static final int MAX_CONCURRENT_TASKS = 2;

    // 默认输出目录：文档/Senlo
    public static final String DEFAULT_OUTPUT_DIR = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "Senlo";

    // 修改后：文件选择器过滤扩展名（已完全同步 SupportedFormats 中图片、音频、视频、文档和压缩包的所有扩展名）
    public static final String[] SUPPORTED_EXTENSIONS = {
            // 视频格式
            "*.mp4", "*.m4v", "*.mkv", "*.mov", "*.avi", "*.wmv", "*.flv", "*.webm",
            "*.3gp", "*.3g2", "*.vob", "*.ts", "*.mts", "*.m2ts", "*.rm", "*.rmvb",
            "*.mpg", "*.mpeg", "*.dv",

            // 音频格式
            "*.mp3", "*.wma", "*.aac", "*.m4a", "*.ogg", "*.opus", "*.flac", "*.alac",
            "*.ape", "*.wav", "*.aiff", "*.aif", "*.amr", "*.ac3", "*.dts",

            // 图片格式
            "*.bmp", "*.gif", "*.jpg", "*.jpeg", "*.jfif", "*.png", "*.tiff", "*.tif",
            "*.webp", "*.avif", "*.heic", "*.heif", "*.apng", "*.psd", "*.svg", "*.ico",
            "*.dng", "*.raw", "*.cr2", "*.nef", "*.eps", "*.pcx", "*.cdr", "*.ufo",

            // 办公文档格式
            "*.docx", "*.doc", "*.dot", "*.dotx", "*.dotm", "*.docm", "*.xlsx", "*.xls",
            "*.xlsm", "*.xltx", "*.xltm", "*.xlt", "*.pptx", "*.ppt", "*.potx", "*.ppsx",
            "*.pdf", "*.odt", "*.ods", "*.odp", "*.txt", "*.rtf", "*.html", "*.htm",
            "*.md", "*.epub", "*.mobi", "*.chm",

            // 压缩包格式
            "*.zip", "*.7z", "*.rar", "*.tar", "*.gz", "*.bz2", "*.jar"
    };

    private Config() {}
}