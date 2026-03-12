package com.campaignworkbench.ide.editor.richtextfx;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;

import java.util.HashSet;
import java.util.Set;

/**
 * Base class for implementing code folding solutions for various languages
 */
public abstract class FoldParser {

    // Collection of foldable paragraphs (start, end, folded state)
    private FoldRegions foldRegions;

    // Used to temporarily store folded state while foldRegions are updated
    protected Set<Integer> foldedParagraphsCache;

    // Used to translate chars to paragraph indices
    CodeArea codeArea;

    public FoldParser(CodeArea codeArea) {
        this.codeArea = codeArea;
        foldRegions = new FoldRegions();
        foldedParagraphsCache = new HashSet<>();
    }

    public void unfoldAll() {
        for (FoldRegion foldRegion : foldRegions) {
            if(foldRegion.isFolded()) {
                unfoldParagraph(foldRegion.getStart());
            }
        }
    }

    public void foldAll() {
        for (FoldRegion foldRegion : foldRegions) {
            if(!foldRegion.isFolded()) {
                foldParagraph(foldRegion.getStart());
            }
        }
    }

    public void foldParagraph(int startParagraphIndex) {
        foldRegions.setParagraphFolded(startParagraphIndex, true);
        int endParagraphIndex = foldRegions.getFoldParagraphEnd(startParagraphIndex);
        codeArea.foldParagraphs(startParagraphIndex, endParagraphIndex );
    }

    public void unfoldParagraph(int startParagraphIndex) {
        foldRegions.setParagraphFolded(startParagraphIndex, false);
        codeArea.unfoldParagraphs(startParagraphIndex);
    }

    public int getFoldParagraphEnd(int paragraphIndex) {
        return foldRegions.getFoldParagraphEnd(paragraphIndex);
    }

    public boolean isParagraphFolded(int paragraphIndex) {
        return foldRegions.isParagraphFolded(paragraphIndex);
    }

    public boolean isParagraphFoldable(int paragraphIndex) {
        return foldRegions.isParagraphFoldable(paragraphIndex);
    }

    /// Return true if paragraphIndex is within one of the folded paragraphs
    public boolean isParagraphHidden(int paragraphIndex) {
        for (FoldRegion foldRegion : foldRegions) {
            if(foldRegion.isParagraphFolded(paragraphIndex)) {
                return true;
            }
        }
        return false;
    }

    public void refresh(){
        foldRegions.clear();
        backupFoldedState();
        updateFoldRegions();
        restoreFoldedState();
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

    private void backupFoldedState() {
        foldedParagraphsCache = new HashSet<>();
        for (FoldRegion foldRegion : foldRegions.values()) {
            if(foldRegion.isFolded()) {
                foldedParagraphsCache.add(foldRegion.getStart());
            }
        }
    }

    private void restoreFoldedState() {
        for(int paragraphIndex : foldedParagraphsCache) {
            foldRegions.getFoldRegion(paragraphIndex).setFolded(true);
        }
    }
}
