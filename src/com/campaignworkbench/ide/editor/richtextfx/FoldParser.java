package com.campaignworkbench.ide.editor.richtextfx;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;

/**
 * Base class for implementing code folding solutions for various languages
 */
public abstract class FoldParser {

    // Collection of foldable paragraphs (start, end, folded state)
    private final FoldRegions foldRegions;

    // Used to translate chars to paragraph indices
    CodeArea codeArea;

    // Debugging
    int refreshCallCount = 0;

    public FoldParser(CodeArea codeArea) {
        this.codeArea = codeArea;
        foldRegions = new FoldRegions();
    }

    public void unfoldAll() {
        for (FoldRegion foldRegion : foldRegions) {
            if(isParagraphFolded(foldRegion.getStart())) {
                unfoldParagraph(foldRegion.getStart());
            }
        }
    }

    public void foldAll() {
        for (FoldRegion foldRegion : foldRegions) {
            if(!isParagraphFolded(foldRegion.getStart())) {
                foldParagraph(foldRegion.getStart());
            }
        }
    }

    public void foldParagraph(int startParagraphIndex) {
        int endParagraphIndex = foldRegions.getFoldParagraphEnd(startParagraphIndex);
        codeArea.foldParagraphs(startParagraphIndex, endParagraphIndex );
    }

    public void unfoldParagraph(int startParagraphIndex) {
        codeArea.unfoldParagraphs(startParagraphIndex);

    }

    public int getFoldParagraphEnd(int paragraphIndex) {
        return foldRegions.getFoldParagraphEnd(paragraphIndex);
    }

    public boolean isParagraphFolded(int paragraphIndex) {
        FoldRegion region = foldRegions.getFoldRegion(paragraphIndex);
        if (region == null) return false;
        return codeArea.isFolded(region.getStart() + 1);
    }

    public boolean isParagraphFoldable(int paragraphIndex) {
        return foldRegions.isParagraphFoldable(paragraphIndex);
    }

    /// Return true if paragraphIndex is within one of the folded paragraphs
    public boolean isParagraphHidden(int paragraphIndex) {
        return codeArea.isFolded(paragraphIndex);
    }

    public void refresh(){
        refreshCallCount ++;
        System.out.println("Refresh call count: " +  refreshCallCount);
        foldRegions.clear();
        updateFoldRegions();
    }

    public abstract void updateFoldRegions();

    // Resolves a paragraph index for a character index
    private int getParagraphFromCharIndex(int charIndex) {
        TwoDimensional.Position pos = codeArea.offsetToPosition(charIndex, TwoDimensional.Bias.Forward);
        return pos.getMajor();
    }

    protected void addFoldRegion(int startChar, int endChar) {
        int startParagraph = getParagraphFromCharIndex(startChar);
        int endParagraph = getParagraphFromCharIndex(endChar);

        foldRegions.add(startParagraph, endParagraph);
    }

}
