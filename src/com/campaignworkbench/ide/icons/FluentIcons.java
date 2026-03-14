package com.campaignworkbench.ide.icons;

import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FluentIcons {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
    private static final Pattern PATH_PATTERN = Pattern.compile("<path[^>]*d=[\"']([^\"']+)[\"'][^>]*/?>");

    public static SVGPath load(String iconName, int size, boolean filled) {
        String rootPath = "/icons";
        String formatPath = "SVG";
        String fileName = "ic_fluent_" + iconName.toLowerCase() + "_" + size + (filled ? "_filled.svg" : "_regular.svg");
        String fullPath = rootPath + "/" + iconName + "/" + formatPath + "/" + fileName;
        String key = fileName + "@" + size;

        String pathData = CACHE.computeIfAbsent(key, k -> {
            try (InputStream in = FluentIcons.class.getResourceAsStream(fullPath)) {
                if (in == null) throw new IllegalArgumentException("Icon not found: " + fullPath);

                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                Matcher m = PATH_PATTERN.matcher(sb);
                if (!m.find()) throw new IllegalArgumentException("No <path> found in SVG: " + fullPath);

                return m.group(1);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load icon: " + fullPath, e);
            }
        });

        SVGPath svg = new SVGPath();
        svg.setContent(pathData);
        svg.getStyleClass().add("icon");
        return svg;
    }

    public static Node icon(String iconName, int size, boolean filled) {
        return load(iconName, size, filled);
    }
}