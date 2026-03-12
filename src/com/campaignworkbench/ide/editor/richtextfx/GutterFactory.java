package com.campaignworkbench.ide.editor.richtextfx;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.fxmisc.richtext.CodeArea;

import java.util.function.IntFunction;

/**
 * A JavaFX node factory that creates a gutter for each paragraph in the CodeArea.
 */
public class GutterFactory implements IntFunction<Node> {

    private final CodeArea codeArea;
    private final IFoldParser foldParser;

    public GutterFactory(CodeArea codeArea, IFoldParser foldParser) {
        this.codeArea = codeArea;
        this.foldParser = foldParser;
        codeArea.setParagraphGraphicFactory(this);
    }

    @Override
    public Node apply(int paragraphIndex) {

        if(foldParser.isParagraphHidden(paragraphIndex)) {
            // System.out.println(paragraphIndex + " is hidden by fold");
            return null;
        }

        // Create an HBox to contain the line number and fold indicator
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(60);
        box.getStyleClass().add("gutter");

        // Get the line number node as a node
        Label lineNo = (Label) SimpleLineNumberFactory.get(codeArea).apply(paragraphIndex);
        lineNo.setMinWidth(36);
        lineNo.getStyleClass().add("line-number");

        // Create a fold indicator label
        Label foldIndicator = new Label();
        foldIndicator.getStyleClass().add("custom-fold-indicator");
        setFoldIndicator(foldIndicator, paragraphIndex);

        // Add the line number and fold indicator to the container and return
        box.getChildren().addAll(lineNo, foldIndicator);
        return box;
    }

    private void setFoldIndicator(Label foldIndicator, int paragraphIndex) {
        if (foldParser.isParagraphFolded(paragraphIndex)) {
            foldIndicator.setText("▶");
            foldIndicator.setCursor(Cursor.HAND);
            foldIndicator.setOnMouseClicked(e -> {
                e.consume();
                foldParser.unfoldParagraph(paragraphIndex);
                // Refresh
                codeArea.setParagraphGraphicFactory(this);
            });
        } else if (foldParser.isParagraphFoldable(paragraphIndex)) {
            foldIndicator.setText("▼");
            foldIndicator.setCursor(Cursor.HAND);
            foldIndicator.setOnMouseClicked(e -> {
                e.consume();
                foldParser.foldParagraph(paragraphIndex);
                // Refresh
                codeArea.setParagraphGraphicFactory(this);
            });
        } else {
            foldIndicator.setText("");
            foldIndicator.setOnMouseClicked(null);
        }
    }
}
