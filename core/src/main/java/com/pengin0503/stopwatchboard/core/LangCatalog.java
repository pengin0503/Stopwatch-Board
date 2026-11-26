package com.pengin0503.stopwatchboard.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads bundled en_us and ja_jp language files from classpath resources so the server
 * can provide localized fallback templates to vanilla clients via translatableWithFallback.
 */
public final class LangCatalog {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Logger LOGGER = Logger.getLogger(LangCatalog.class.getName());

    private final Map<String, String> enUs;
    private final Map<String, String> jaJp;

    public LangCatalog(Map<String, String> enUs, Map<String, String> jaJp) {
        this.enUs = enUs == null ? Map.of() : Collections.unmodifiableMap(enUs);
        this.jaJp = jaJp == null ? Map.of() : Collections.unmodifiableMap(jaJp);
    }

    public static LangCatalog loadFromClasspath(ClassLoader classLoader) {
        Map<String, String> en = loadResource(classLoader, "assets/stopwatchboard/lang/en_us.json");
        Map<String, String> ja = loadResource(classLoader, "assets/stopwatchboard/lang/ja_jp.json");
        return new LangCatalog(en, ja);
    }

    private static Map<String, String> loadResource(ClassLoader classLoader, String path) {
        try (InputStream stream = classLoader.getResourceAsStream(path)) {
            if (stream == null) {
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Map<String, String> parsed = GSON.fromJson(reader, MAP_TYPE);
                return parsed == null ? Map.of() : parsed;
            }
        } catch (Exception e) {
            LOGGER.warning("[StopwatchBoard] Failed to load language resource '" + path + "': " + e.getMessage());
            return Map.of();
        }
    }

    /**
     * Resolves the fallback translation pattern for {@code key} given a Minecraft client locale
     * (e.g. {@code ja_jp}, {@code en_us}). Falls back to {@code en_us}, then to {@code key}.
     */
    public String getFallback(String clientLocale, String key) {
        if (isJapanese(clientLocale)) {
            String ja = jaJp.get(key);
            if (ja != null) {
                return ja;
            }
        }
        return enUs.getOrDefault(key, key);
    }

    public static boolean isJapanese(String clientLocale) {
        if (clientLocale == null || clientLocale.isBlank()) {
            return false;
        }
        String normalized = clientLocale.toLowerCase(Locale.ROOT);
        return normalized.equals("ja") || normalized.startsWith("ja_") || normalized.startsWith("ja-");
    }
}
