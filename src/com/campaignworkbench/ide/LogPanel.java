package com.campaignworkbench.ide;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * User interface control to provided a logging console
 */
public class LogPanel implements IJavaFxNode {

    private static RichTextArea logArea;
    // Main panel
    static VBox logPanel;

    private static final StyleAttributeMap timestampStyle =
            StyleAttributeMap.builder()
                    .setTextColor(javafx.scene.paint.Color.web("#888888"))
                    .build();

    private static final StyleAttributeMap logEntryStyle =
            StyleAttributeMap.builder()
                    // .setTextColor(javafx.scene.paint.Color.web("#b3b3b3"))
                    .build();

    private static final DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor
     *
     * @param label The label for the log panel
     */
    public LogPanel(String label) {
        Label logLabel = new Label(label);
        logLabel.setPadding(new Insets(0, 0, 0, 5));

        logArea = new RichTextArea();
        logArea.setEditable(false);
        logArea.setWrapText(false);
        logArea.setCursor(Cursor.TEXT); // full-width log pane
        logArea.getStyleClass().add("log-area");


        logPanel = new VBox(5, logLabel, logArea);
        logPanel.setPadding(new Insets(0, 0, 0, 5));
        logPanel.setMinHeight(0);
        logPanel.getStyleClass().add("log-panel");
        VBox.setVgrow(logArea, Priority.ALWAYS);
    }

    public Node getNode() {
        return logPanel;
    }

    /**
     * Adds a line of content to the log
     *
     * @param message text containing content of the log line to add
     */
    public void addLogEntry(String message) {

        String timeStamp = LocalDateTime.now().format(timestampFormat);
        logArea.appendText(timeStamp + ": ", timestampStyle);
        logArea.appendText(message, logEntryStyle);
        logArea.appendText("\n");
    }
}
