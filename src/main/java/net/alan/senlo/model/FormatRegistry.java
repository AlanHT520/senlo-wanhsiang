package net.alan.senlo.model;

import java.util.*;

/**
 * 定义“森罗万象”软件支持的所有文件格式。
 * 按类别分组：图片、音频、视频、办公文档、压缩包。
 */
public final class FormatRegistry {

    // -------------------- 图片格式 --------------------
    public static final List<FormatInfo> IMAGE_FORMATS = List.of(
            new FormatInfo("BMP", ".bmp", "image", Arrays.asList(".bmp")),
            new FormatInfo("GIF", ".gif", "image", Arrays.asList(".gif")),
            new FormatInfo("JPEG", ".jpg", "image", Arrays.asList(".jpg", ".jpeg", ".jfif")),
            new FormatInfo("PNG", ".png", "image", Arrays.asList(".png")),
            new FormatInfo("TIFF", ".tiff", "image", Arrays.asList(".tiff", ".tif")),
            new FormatInfo("WebP", ".webp", "image", Arrays.asList(".webp")),
            new FormatInfo("AVIF", ".avif", "image", Arrays.asList(".avif")),
            new FormatInfo("HEIC", ".heic", "image", Arrays.asList(".heic", ".heif")),
            new FormatInfo("APNG", ".apng", "image", Arrays.asList(".apng")),
            new FormatInfo("PSD", ".psd", "image", Arrays.asList(".psd")),
            new FormatInfo("SVG", ".svg", "image", Arrays.asList(".svg")),
            new FormatInfo("ICO", ".ico", "image", Arrays.asList(".ico")),
            new FormatInfo("DNG", ".dng", "image", Arrays.asList(".dng")),
            new FormatInfo("RAW", ".raw", "image", Arrays.asList(".raw", ".cr2", ".nef")),
            new FormatInfo("EPS", ".eps", "image", Arrays.asList(".eps")),
            new FormatInfo("PCX", ".pcx", "image", Arrays.asList(".pcx")),
            new FormatInfo("CDR", ".cdr", "image", Arrays.asList(".cdr")),
            new FormatInfo("UFO", ".ufo", "image", Arrays.asList(".ufo"))
    );

    // -------------------- 音频格式 --------------------
    public static final List<FormatInfo> AUDIO_FORMATS = List.of(
            new FormatInfo("MP3", ".mp3", "audio", Arrays.asList(".mp3")),
            new FormatInfo("WMA", ".wma", "audio", Arrays.asList(".wma")),
            new FormatInfo("AAC", ".aac", "audio", Arrays.asList(".aac")),
            new FormatInfo("M4A", ".m4a", "audio", Arrays.asList(".m4a")),
            new FormatInfo("OGG", ".ogg", "audio", Arrays.asList(".ogg")),
            new FormatInfo("Opus", ".opus", "audio", Arrays.asList(".opus")),
            new FormatInfo("FLAC", ".flac", "audio", Arrays.asList(".flac")),
            new FormatInfo("ALAC", ".alac", "audio", Arrays.asList(".alac")),
            new FormatInfo("APE", ".ape", "audio", Arrays.asList(".ape")),
            new FormatInfo("WAV", ".wav", "audio", Arrays.asList(".wav")),
            new FormatInfo("AIFF", ".aiff", "audio", Arrays.asList(".aiff", ".aif")),
            new FormatInfo("AMR", ".amr", "audio", Arrays.asList(".amr")),
            new FormatInfo("AC3", ".ac3", "audio", Arrays.asList(".ac3")),
            new FormatInfo("DTS", ".dts", "audio", Arrays.asList(".dts"))
    );

    // -------------------- 视频格式 --------------------
    public static final List<FormatInfo> VIDEO_FORMATS = List.of(
            new FormatInfo("MP4", ".mp4", "video", Arrays.asList(".mp4", ".m4v")),
            new FormatInfo("MKV", ".mkv", "video", Arrays.asList(".mkv")),
            new FormatInfo("MOV", ".mov", "video", Arrays.asList(".mov")),
            new FormatInfo("AVI", ".avi", "video", Arrays.asList(".avi")),
            new FormatInfo("WMV", ".wmv", "video", Arrays.asList(".wmv")),
            new FormatInfo("FLV", ".flv", "video", Arrays.asList(".flv")),
            new FormatInfo("WebM", ".webm", "video", Arrays.asList(".webm")),
            new FormatInfo("3GP", ".3gp", "video", Arrays.asList(".3gp", ".3g2")),
            new FormatInfo("VOB", ".vob", "video", Arrays.asList(".vob")),
            new FormatInfo("TS", ".ts", "video", Arrays.asList(".ts", ".mts", ".m2ts")),
            new FormatInfo("RMVB", ".rmvb", "video", Arrays.asList(".rm", ".rmvb")),
            new FormatInfo("MPEG", ".mpg", "video", Arrays.asList(".mpg", ".mpeg")),
            new FormatInfo("GIF动画", ".gif", "video", Arrays.asList(".gif")),
            new FormatInfo("DV", ".dv", "video", Arrays.asList(".dv"))
    );

    // -------------------- 办公文档格式 --------------------
    public static final List<FormatInfo> DOCUMENT_FORMATS = List.of(
            new FormatInfo("Word 文档", ".docx", "document", Arrays.asList(".docx", ".doc", ".dot", ".dotx", ".dotm", ".docm", ".rtf")),
            new FormatInfo("Excel 工作簿", ".xlsx", "document", Arrays.asList(".xlsx", ".xls", ".xlsm", ".xltx", ".xltm", ".xlt")),
            new FormatInfo("PowerPoint 演示文稿", ".pptx", "document", Arrays.asList(".pptx", ".ppt", ".odp", ".potx", ".ppsx")),
            new FormatInfo("PDF 文档", ".pdf", "document", Arrays.asList(".pdf")),
            new FormatInfo("开放文档文本", ".odt", "document", Arrays.asList(".odt")),
            new FormatInfo("开放文档表格", ".ods", "document", Arrays.asList(".ods")),
            new FormatInfo("开放文档演示", ".odp", "document", Arrays.asList(".odp")),
            new FormatInfo("纯文本", ".txt", "document", Arrays.asList(".txt")),
            new FormatInfo("富文本", ".rtf", "document", Arrays.asList(".rtf")),
            new FormatInfo("HTML 网页", ".html", "document", Arrays.asList(".html", ".htm")),
            new FormatInfo("Markdown", ".md", "document", Arrays.asList(".md")),
            new FormatInfo("EPUB 电子书", ".epub", "document", Arrays.asList(".epub")),
            new FormatInfo("MOBI 电子书", ".mobi", "document", Arrays.asList(".mobi", ".azw", ".azw3")),
            new FormatInfo("CHM 帮助文档", ".chm", "document", Arrays.asList(".chm"))
    );

    // -------------------- 压缩包格式 --------------------
    public static final List<FormatInfo> ARCHIVE_FORMATS = List.of(
            new FormatInfo("ZIP 压缩包", ".zip", "archive", Arrays.asList(".zip")),
            new FormatInfo("7-Zip 压缩包", ".7z", "archive", Arrays.asList(".7z")),
            new FormatInfo("RAR 压缩包", ".rar", "archive", Arrays.asList(".rar")),
            new FormatInfo("TAR 归档包", ".tar", "archive", Arrays.asList(".tar")),
            new FormatInfo("GZIP 压缩包", ".gz", "archive", Arrays.asList(".gz", ".gzip")),
            new FormatInfo("BZIP2 压缩包", ".bz2", "archive", Arrays.asList(".bz2")),
            new FormatInfo("JAR 包", ".jar", "archive", Arrays.asList(".jar"))
    );

    public static final List<FormatInfo> ALL_FORMATS;
    static {
        List<FormatInfo> all = new ArrayList<>();
        all.addAll(IMAGE_FORMATS);
        all.addAll(AUDIO_FORMATS);
        all.addAll(VIDEO_FORMATS);
        all.addAll(DOCUMENT_FORMATS);
        all.addAll(ARCHIVE_FORMATS);
        ALL_FORMATS = Collections.unmodifiableList(all);
    }

    /**
     * 根据文件扩展名（带点或不带点）获取对应的 FormatInfo。
     */
    public static FormatInfo fromExtension(String extension) {
        String ext = extension.toLowerCase();
        if (!ext.startsWith(".")) ext = "." + ext;
        for (FormatInfo info : ALL_FORMATS) {
            if (info.getExtensions().contains(ext)) return info;
        }
        return null;
    }

    /**
     * 获取所有输出格式名称列表（用于全局显示，不建议直接用于格式下拉框）。
     */
    public static List<String> getAllOutputFormatNames() {
        List<String> names = new ArrayList<>();
        for (FormatInfo info : ALL_FORMATS) {
            names.add(info.getName());
        }
        return names;
    }

    /**
     * 根据格式名称获取对应的 FormatInfo。
     */
    public static FormatInfo getByName(String name) {
        for (FormatInfo info : ALL_FORMATS) {
            if (info.getName().equals(name)) return info;
        }
        return null;
    }

    /**
     * 新增：根据媒体类别返回格式名称列表（用于动态过滤下拉框）。
     * @param category "video", "audio", "image", "document", "archive"
     */
    public static List<String> getFormatNamesByCategory(String category) {
        List<String> names = new ArrayList<>();
        for (FormatInfo info : ALL_FORMATS) {
            if (info.getCategory().equals(category)) {
                names.add(info.getName());
            }
        }
        return names;
    }

    // -------------------- 内部辅助类 --------------------
    public static class FormatInfo {
        private final String name;
        private final String primaryExtension;
        private final String category;
        private final List<String> extensions;

        public FormatInfo(String name, String primaryExtension, String category, List<String> extensions) {
            this.name = name;
            this.primaryExtension = primaryExtension;
            this.category = category;
            this.extensions = Collections.unmodifiableList(extensions);
        }

        public String getName() { return name; }
        public String getPrimaryExtension() { return primaryExtension; }
        public String getCategory() { return category; }
        public List<String> getExtensions() { return extensions; }

        @Override
        public String toString() {
            return name + " (" + primaryExtension + ")";
        }
    }
}