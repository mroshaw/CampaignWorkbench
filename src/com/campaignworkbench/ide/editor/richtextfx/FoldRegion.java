package com.campaignworkbench.ide.editor.richtextfx;

/**
 * Class representing the start and end paragraph of a region of foldable code
 */
public class FoldRegion {

    private final int startParagraphIndex;
    private final int endParagraphIndex;
    private boolean isFolded;

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

    public void fold() {
        setFolded(true);
    }

    public void unfold() {
        setFolded(false);
    }

    public void setFolded(boolean isFolded) {
        this.isFolded = isFolded;
    }

    public boolean isFolded() {
        return isFolded;
    }

    public boolean isParagraphFolded(int paragraphIndex) {
        return isParagraphWithin(paragraphIndex) && isFolded;
    }

    private boolean isParagraphWithin(int paragraphIndex) {
        return paragraphIndex > startParagraphIndex && paragraphIndex <= endParagraphIndex;
    }

    private void backupFolded() {

    }
}
