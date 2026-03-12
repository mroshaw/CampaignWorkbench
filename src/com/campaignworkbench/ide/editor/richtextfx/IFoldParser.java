package com.campaignworkbench.ide.editor.richtextfx;

/**
 * Interface describing a class that folds and unfolds regions of code
 */
public interface IFoldParser {
    /**
     * Returns foldable regions as (startParagraph, endParagraph) pairs.
     */
    void updateFoldRegions();

    boolean isParagraphFolded(int paragraphIndex);
    boolean isParagraphHidden(int paragraphIndex);
    boolean isParagraphFoldable(int paragraphIndex);

    int getFoldParagraphEnd(int paragraphIndex);

    void foldParagraph(int startParagraphIndex);
    void unfoldParagraph(int startParagraphIndex);
    void foldAll();
    void unfoldAll();
    void refresh();
}
