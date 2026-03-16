package com.campaignworkbench.ide.editor.richtextfx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import org.fxmisc.richtext.CodeArea;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * A JavaFX node factory that creates a gutter node for each paragraph in the CodeArea,
 * containing a line number and fold indicator.
 */
public class GutterFactory implements IntFunction<Node> {

    private static final Insets DEFAULT_INSETS =
            new Insets(0.0, 5.0, 0.0, 5.0);

    private static final Paint DEFAULT_TEXT_FILL =
            Color.web("#666");

    private static final Font DEFAULT_FONT =
            Font.font("monospace", FontPosture.ITALIC, 13.0);

    private static final Background DEFAULT_BACKGROUND =
            new Background(
                    new BackgroundFill(
                            Color.web("#ddd"),
                            null,
                            null
                    )
            );

    private final Map<Integer, HBox> nodeCache = new HashMap<>();
    private final Map<Integer, Label> foldIndicatorCache = new HashMap<>();

    private final CodeArea codeArea;
    private final IFoldParser foldParser;
    private final Val<Integer> nParagraphs;
    private final IntFunction<String> format;

    public GutterFactory(CodeArea codeArea, IFoldParser foldParser) {
        this(codeArea, foldParser, digits -> "%1$" + digits + "s");
    }

    public GutterFactory(CodeArea codeArea, IFoldParser foldParser, IntFunction<String> format) {
        this.codeArea = codeArea;
        this.foldParser = foldParser;
        this.format = format;
        this.nParagraphs = LiveList.sizeOf(codeArea.getParagraphs());
        codeArea.setParagraphGraphicFactory(this);
    }

    @Override
    public Node apply(int paragraphIndex) {

        if (foldParser == null || foldParser.isParagraphHidden(paragraphIndex)) {
            return null;
        }

        Val<String> formatted = nParagraphs.map(n -> format(paragraphIndex + 1, n));

        Label lineNo = new Label();
        lineNo.setFont(DEFAULT_FONT);
        lineNo.setBackground(DEFAULT_BACKGROUND);
        lineNo.setTextFill(DEFAULT_TEXT_FILL);
        lineNo.setPadding(DEFAULT_INSETS);
        lineNo.setAlignment(Pos.TOP_RIGHT);
        lineNo.getStyleClass().add("line-number");
        lineNo.textProperty().bind(formatted.conditionOnShowing(lineNo));

        Label foldIndicator = new Label();
        foldIndicator.getStyleClass().add("custom-fold-indicator");
        setFoldIndicator(foldIndicator, paragraphIndex);

        HBox box = new HBox(lineNo, foldIndicator);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(60);
        box.getStyleClass().add("gutter");

        return box;
    }

    public void refresh(int fromParagraph, int toParagraph) {
        for (int i = fromParagraph; i <= toParagraph; i++) {
            codeArea.recreateParagraphGraphic(i);
        }
    }

    private void setFoldIndicator(Label foldIndicator, int paragraphIndex) {
        if (foldParser.isParagraphFolded(paragraphIndex)) {
            foldIndicator.setText("▶");
            foldIndicator.setCursor(Cursor.HAND);
            foldIndicator.setOnMouseClicked(e -> {
                e.consume();
                foldParser.unfoldParagraph(paragraphIndex);
                refresh(paragraphIndex, foldParser.getFoldParagraphEnd(paragraphIndex));
            });
        } else if (foldParser.isParagraphFoldable(paragraphIndex)) {
            foldIndicator.setText("▼");
            foldIndicator.setCursor(Cursor.HAND);
            foldIndicator.setOnMouseClicked(e -> {
                e.consume();
                foldParser.foldParagraph(paragraphIndex);
                refresh(paragraphIndex, foldParser.getFoldParagraphEnd(paragraphIndex));
            });
        } else {
            foldIndicator.setText("");
            foldIndicator.setOnMouseClicked(null);
        }
    }

    private String format(int x, int max) {
        int digits = (int) Math.floor(Math.log10(max)) + 1;
        return String.format(format.apply(digits), x);
    }
}