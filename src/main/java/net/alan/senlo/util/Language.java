package net.alan.senlo.util;

import net.alan.senlo.model.ApplicationSettings;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Language {
    private static Map<String, String> currentMessages = new HashMap<>();

    static {
        load();
    }

    public static void load() {
        String lang = ApplicationSettings.getInstance().getLanguage(); // "en_us" 或 "zh_cn"
        String resourcePath = "/lang/" + lang + ".properties";
        Properties props = new Properties();
        try (InputStream in = Language.class.getResourceAsStream(resourcePath);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            if (in == null) {
                System.err.println("Language file not found: " + resourcePath + ", fallback to en_us");
                try (InputStream fallback = Language.class.getResourceAsStream("/lang/en_us.properties");
                     InputStreamReader fallbackReader = new InputStreamReader(fallback, StandardCharsets.UTF_8)) {
                    if (fallback != null) props.load(fallbackReader);
                }
            } else {
                props.load(reader);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        currentMessages.clear();
        for (String key : props.stringPropertyNames()) {
            currentMessages.put(key, props.getProperty(key));
        }
    }

    public static String get(String key) {
        return currentMessages.getOrDefault(key, key);
    }
}