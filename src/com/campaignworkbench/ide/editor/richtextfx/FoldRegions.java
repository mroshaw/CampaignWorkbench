package com.campaignworkbench.ide.editor.richtextfx;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Class implementing a set of fold regions
 */
public class FoldRegions implements Iterable<FoldRegion> {

    private final HashMap<Integer, FoldRegion> foldRegionMap;

    public FoldRegions() {
        foldRegionMap = new HashMap<>();
    }

    public FoldRegion getFoldRegion(int paragraphIndex) {
        return foldRegionMap.get(paragraphIndex);
    }

    public void add(int startParagraphIndex, int endParagraphIndex) {

        // Check if it's already there or start and end are the same
        if (foldRegionMap.containsKey(startParagraphIndex) || startParagraphIndex == endParagraphIndex) {
            return;
        }
        FoldRegion newRegion = new FoldRegion(startParagraphIndex, endParagraphIndex);
        foldRegionMap.put(startParagraphIndex, newRegion);
    }

    public void remove(int paragraphIndex) {
        // If not in the map, do nothing
        if (!foldRegionMap.containsKey(paragraphIndex)) {
            return;
        }
        foldRegionMap.remove(paragraphIndex);
    }

    public void clear() {
        foldRegionMap.clear();
    }

    public void setParagraphFolded(int paragraphIndex, boolean isFolded) {
        getFoldRegion(paragraphIndex).setFolded(isFolded);
    }

    public int getFoldParagraphEnd(int paragraphIndex) {
        if (!foldRegionMap.containsKey(paragraphIndex)) {
            return paragraphIndex;
        }

        return foldRegionMap.get(paragraphIndex).getEnd();
    }

    public boolean isParagraphFoldable(int paragraphIndex) {
        return foldRegionMap.containsKey(paragraphIndex);
    }

    public boolean isParagraphFolded(int paragraphIndex) {
        FoldRegion foldRegion = foldRegionMap.get(paragraphIndex);
        if(foldRegion == null) {
            return false;
        }
        return foldRegion.isFolded();
    }

    @Override
    public Iterator<FoldRegion> iterator() {
        return foldRegionMap.values().iterator();
    }

    public Iterable<FoldRegion> values() {
        return foldRegionMap.values();
    }

}
