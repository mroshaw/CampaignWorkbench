package com.campaignworkbench.ide.icons;

import javafx.scene.Node;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FluentIcons {

    private static final Path rootPath = Paths.get("/icons");
    private static final Path formatPath = Paths.get("SVG");
    private static final Map<String, SVGPath> CACHE = new ConcurrentHashMap<>();
    private static final Pattern PATH_PATTERN = Pattern.compile("<path[^>]*d=[\"']([^\"']+)[\"'][^>]*/?>");

    /**
     * Load a Fluent icon from resources/icons folder.
     *
     */
    public static SVGPath load(String iconName, int size, boolean filled) {
        // Use cache key based on name + size
        String rootPath ="/icons";
        String formatPath = "SVG";

        String fileName = "ic_fluent_" + iconName.toLowerCase() + "_" + size + (filled ? "_filled.svg" : "_regular.svg") ;
        String fullPath = rootPath + "/" + iconName + "/" + formatPath + "/" + fileName;

        String key = fileName + "@" + size;
        return CACHE.computeIfAbsent(key, k -> {
            String fullPathStr = fullPath.toString();
            System.out.println("Loading icon: " + fullPathStr);
            try (InputStream in = FluentIcons.class.getResourceAsStream(fullPathStr)) {
                if (in == null) throw new IllegalArgumentException("Icon not found: " + fullPath);

                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                Matcher m = PATH_PATTERN.matcher(sb);
                if (!m.find()) throw new IllegalArgumentException("No <path> found in SVG: " + fullPath);

                String pathData = m.group(1);
                SVGPath svg = new SVGPath();
                svg.setContent(pathData);
                svg.getStyleClass().add("icon");

                // Optional scaling
                if (size > 0) {
                    double viewBoxSize = 16.0; // default; could parse viewBox if needed
                    double scaleFactor = size / viewBoxSize;
                    svg.getTransforms().add(new Scale(scaleFactor, scaleFactor));
                }

                return svg;

            } catch (Exception e) {
                throw new RuntimeException("Failed to load icon: " + fullPath, e);
            }
        });
    }

    /**
     * Return as Node for direct UI use
     */
    public static Node icon(String iconName, int size, boolean filled) {
        return load(iconName, size, filled);
    }

}