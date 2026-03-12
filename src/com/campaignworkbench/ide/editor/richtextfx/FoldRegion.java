package com.campaignworkbench.ide.editor.richtextfx;

/**
 * Class representing the start and end paragraph of a region of foldable code
 */
public class FoldRegion {

    private final int startParagraphIndex;
    private final int endParagraphIndex;

    public FoldRegion(int startParagraphIndex, int endParagraphIndex) {
        this.startParagraphIndex = startParagraphIndex;
        this.endParagraphIndex = endParagraphIndex;
    }

    public int getStart() {
        return startParagraphIndex;
    }

    public int getEnd() {
        return endParagraphIndex;
    }

    public boolean isParagraphWithin(int paragraphIndex) {
        return paragraphIndex > startParagraphIndex && paragraphIndex <= endParagraphIndex;
    }
}
