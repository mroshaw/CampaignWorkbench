package com.campaignworkbench.ide.editor.richtextfx;

import com.campaignworkbench.ide.IdeException;
import com.campaignworkbench.ide.editor.ICodeEditor;
import com.campaignworkbench.ide.editor.SyntaxType;
import com.campaignworkbench.ide.editor.richtextfx.codeformatting.CampaignFormatter;
import com.campaignworkbench.ide.editor.richtextfx.codeformatting.ICodeFormatter;
import com.campaignworkbench.ide.editor.richtextfx.codeformatting.JavaScriptFormatter;
import com.campaignworkbench.ide.editor.richtextfx.codeformatting.XmlFormatter;
import com.campaignworkbench.ide.editor.richtextfx.folding.CampaignFoldParser;
import com.campaignworkbench.ide.editor.richtextfx.folding.HtmlFoldParser;
import com.campaignworkbench.ide.editor.richtextfx.folding.JavaScriptFoldParser;
import com.campaignworkbench.ide.editor.richtextfx.folding.XmlFoldParser;
import com.campaignworkbench.ide.editor.richtextfx.syntaxhighlighting.*;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.TwoDimensional;
import org.reactfx.Subscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of ICodeEditor using the RichTextFX library
 */
public class RichTextFXEditor implements ICodeEditor {

    private final CodeArea codeArea;
    private final BorderPane root;

    // Syntax highlighting
    private ISyntaxStyler syntaxStyler;

    // Code Folding
    private IFoldParser foldParser;

    // Code formatting
    private ICodeFormatter codeFormatter;

    /**
     * Constructor
     */
    public RichTextFXEditor(SyntaxType syntaxType) {
        codeArea = new CodeArea();
        codeArea.setCursor(Cursor.TEXT);
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        root = new BorderPane(scrollPane);
        root.getStyleClass().add("code-editor");

        setLanguageHelpers(syntaxType);

        GutterFactory gutterFactory = new GutterFactory(codeArea, foldParser);
        codeArea.setParagraphGraphicFactory(gutterFactory);

        // When text changes
        codeArea.textProperty().addListener((_, _, newText) -> {
            // Re-evaluate and apply styling
            if (syntaxStyler != null) {
                StyleSpans<Collection<String>> computedStyleSpans = syntaxStyler.style(newText);
                if (computedStyleSpans != null) {
                    codeArea.setStyleSpans(0, computedStyleSpans);
                }
            }

            // Re-evaluate and apply folding
            if (foldParser != null) {
                foldParser.refresh();
            }
        });

        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() != KeyCode.TAB) return;
            e.consume();

            boolean dedent = e.isShiftDown();
            IndexRange selection = codeArea.getSelection();

            // No selection → insert or remove two spaces at caret
            if (selection.getLength() == 0) {
                if (!dedent) {
                    codeArea.replaceSelection("  ");
                } else {
                    // Remove up to 2 spaces before caret
                    int caretPos = codeArea.getCaretPosition();
                    int col = codeArea.offsetToPosition(caretPos, TwoDimensional.Bias.Backward).getMinor();
                    String lineText = codeArea.getParagraph(codeArea.getCurrentParagraph()).getText();
                    int spacesToRemove = 0;
                    for (int i = 0; i < 2 && (col - spacesToRemove) > 0 && lineText.charAt(col - spacesToRemove - 1) == ' '; i++) {
                        spacesToRemove++;
                    }
                    if (spacesToRemove > 0) {
                        codeArea.deleteText(caretPos - spacesToRemove, caretPos);
                    }
                }
                return;
            }

            int startPar = codeArea.offsetToPosition(selection.getStart(), TwoDimensional.Bias.Forward).getMajor();
            int endPar = codeArea.offsetToPosition(selection.getEnd(), TwoDimensional.Bias.Backward).getMajor();

            if (!dedent) {
                // Indent — insert two spaces at the start of each line
                var multi = codeArea.createMultiChange();
                for (int i = startPar; i <= endPar; i++) {
                    int lineStart = codeArea.getAbsolutePosition(i, 0);
                    multi.insertText(lineStart, "  ");
                }
                multi.commit();

                int lines = endPar - startPar + 1;
                codeArea.selectRange(
                        selection.getStart(),
                        selection.getEnd() + (lines * 2)
                );
            } else {
                // Dedent — remove up to 2 leading spaces from each line
                var multi = codeArea.createMultiChange();
                int[] spacesRemovedPerLine = new int[endPar - startPar + 1];
                boolean anyChanges = false;

                for (int i = startPar; i <= endPar; i++) {
                    String lineText = codeArea.getParagraph(i).getText();
                    int spacesToRemove = 0;
                    for (int j = 0; j < 2 && j < lineText.length() && lineText.charAt(j) == ' '; j++) {
                        spacesToRemove++;
                    }
                    spacesRemovedPerLine[i - startPar] = spacesToRemove;
                    if (spacesToRemove > 0) {
                        int lineStart = codeArea.getAbsolutePosition(i, 0);
                        multi.deleteText(lineStart, lineStart + spacesToRemove);
                        anyChanges = true;
                    }
                }

                if (!anyChanges) return;

                multi.commit();

                // Restore selection shrunk by spaces removed
                int firstLineRemoved = spacesRemovedPerLine[0];
                int totalRemoved = 0;
                for (int s : spacesRemovedPerLine) totalRemoved += s;

                int newStart = Math.max(0, selection.getStart() - firstLineRemoved);
                int newEnd = Math.max(newStart, selection.getEnd() - totalRemoved);
                codeArea.selectRange(newStart, newEnd);
            }
        });
    }

    @Override
    public Node getNode() {
        return root;
    }

    @Override
    public void refreshContent() {
        // RichTextFX usually handles its own repainting
    }

    @Override
    public void setText(String text) {
        String newText = text.replace("\t", "  ");
        codeArea.replaceText(newText);

        if (syntaxStyler == null) {
            return;
        }
        StyleSpans<Collection<String>> computedStyleSpans = syntaxStyler.style(codeArea.getText());
        if (computedStyleSpans != null) {
            codeArea.setStyleSpans(0, computedStyleSpans);
        }

    }

    @Override
    public String getText() {
        return codeArea.getText();
    }

    private void setLanguageHelpers(SyntaxType syntax) {
        // Set the styler, folder and formatter class instances
        switch (syntax) {
            case XML:
                codeFormatter = new XmlFormatter();
                syntaxStyler = new XmlStyler();
                foldParser = new XmlFoldParser(codeArea);
                break;
            case MODULE:
                codeFormatter = new CampaignFormatter();
                syntaxStyler = new CampaignSyntaxStyler();
                foldParser = new CampaignFoldParser(codeArea);
                break;
            case BLOCK:
                codeFormatter = new CampaignFormatter();
                syntaxStyler = new CampaignSyntaxStyler();
                foldParser = new CampaignFoldParser(codeArea);
                break;
            case TEMPLATE:
                codeFormatter = new CampaignFormatter();
                syntaxStyler = new CampaignSyntaxStyler();
                foldParser = new CampaignFoldParser(codeArea);
                break;
            case JAVASCRIPT:
                codeFormatter = new CampaignFormatter();
                syntaxStyler = new JavaScriptSyntaxStyler();
                foldParser = new JavaScriptFoldParser(codeArea);
                break;
            case HTML:
                codeFormatter = new JavaScriptFormatter();
                syntaxStyler = new HtmlSyntaxStyler();
                foldParser = new HtmlFoldParser(codeArea);
                break;
        }
    }

    @Override
    public void setEditable(boolean editable) {
        codeArea.setEditable(editable);
    }

    @Override
    public void setWrap(boolean wrap) {
        codeArea.setWrapText(wrap);
    }

    @Override
    public void requestFocus() {
        codeArea.requestFocus();
    }

    @Override
    public void setCaretAtStart() {
        codeArea.moveTo(0);
        codeArea.requestFollowCaret();
    }

    @Override
    public void gotoLine(int line) {
        if (line <= 0) return;
        int paragraphIndex = Math.min(line - 1, codeArea.getParagraphs().size() - 1);
        codeArea.moveTo(paragraphIndex, 0);
        codeArea.selectLine();
        codeArea.requestFollowCaret();
    }

    @Override
    public void insertTextAtCaret(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int caretPos = codeArea.getCaretPosition();

        // If there's a selection, replace it
        if (codeArea.getSelection().getLength() > 0) {
            codeArea.replaceText(codeArea.getSelection(), text);
        } else {
            codeArea.insertText(caretPos, text);
        }

        // Move caret to after inserted text
        codeArea.moveTo(caretPos + text.length());
    }

    public void formatCode(int indentSize) {
        String formattedCode = "";
        if (codeFormatter == null || getText().isEmpty()) {
            return;
        }
        try {
            formattedCode = codeFormatter.format(getText(), indentSize);
        } catch (Exception ex) {
            throw new IdeException("Error formatting code", ex);
        }
        if (formattedCode.isEmpty()) {
            throw new IdeException("Unexpected error formatting code", null);
        }
        setText(formattedCode);
    }

    @Override
    public void foldAll() {
        foldParser.foldAll();
    }

    @Override
    public void unfoldAll() {
        foldParser.unfoldAll();
    }

    @Override
    public Subscription addTextChangeListener(Consumer<String> listener) {
        return codeArea.multiPlainChanges()
                .subscribe(change -> listener.accept(codeArea.getText()));
    }

    @Override
    public void find(String text) {
        if (text == null || text.isEmpty()) {
            clearFindHighlight();
            return;
        }

        String content = codeArea.getText();
        Pattern pattern = Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            for (int i = start; i < end; i++) {
                codeArea.setStyle(i, i + 1, mergeStyles(codeArea.getStyleOfChar(i)));
            }
        }
    }

    public void clearFindHighlight() {

        int length = codeArea.getLength();

        for (int i = 0; i < length; i++) {

            Collection<String> styles = codeArea.getStyleOfChar(i);

            if (styles.contains("find-text")) {

                List<String> cleaned =
                        styles.stream()
                                .filter(s -> !s.equals("find-text"))
                                .toList();

                codeArea.setStyle(i, i + 1, cleaned);
            }
        }
    }

    /**
     * Returns a new style collection containing the original styles plus the highlight
     */
    private Collection<String> mergeStyles(Collection<String> original) {
        if (original.contains("find-text")) return original; // already highlighted
        ArrayList<String> merged = new ArrayList<>(original);
        merged.add("find-text");
        return merged;
    }
}
