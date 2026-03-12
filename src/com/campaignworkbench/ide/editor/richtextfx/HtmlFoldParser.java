package com.campaignworkbench.ide.editor.richtextfx;

import org.fxmisc.richtext.CodeArea;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of IFoldParser that identifies fold regions in HTML code.
 * Handles void elements, HTML comments, and script/style blocks.
 */
public class HtmlFoldParser extends FoldParser implements IFoldParser {

    // Void elements that never have a closing tag
    private static final Set<String> VOID_ELEMENTS = Set.of(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
    );

    // Matches opening tags, closing tags, self-closing tags, and comments
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(<!--.*?-->)" +                          // group 1: comment (non-greedy, single-line)
                    "|(<!--)" +                               // group 2: comment open (multi-line)
                    "|(-->)" +                                // group 3: comment close
                    "|<(/?)([a-zA-Z][a-zA-Z0-9_:-]*)((?:[^>])*)>", // groups 4,5,6: slash, tag name, rest
            Pattern.DOTALL
    );

    public HtmlFoldParser(CodeArea codeArea) {
        super(codeArea);
    }

    @Override
    public FoldRegions findFoldRegions() {

        foldRegions = new FoldRegions();
        String text = codeArea.getText();

        Matcher matcher = TOKEN_PATTERN.matcher(text);
        Deque<Tag> stack = new ArrayDeque<>();
        boolean inComment = false;
        int commentStart = -1;

        while (matcher.find()) {

            if (matcher.group(1) != null) {
                // Single-line comment: <!-- ... --> - fold if it spans multiple lines
                addFoldRegion(matcher.start(), matcher.end());
                continue;
            }

            if (matcher.group(2) != null) {
                // Multi-line comment open <!--
                inComment = true;
                commentStart = matcher.start();
                continue;
            }

            if (matcher.group(3) != null) {
                // Comment close -->
                if (inComment) {
                    addFoldRegion(commentStart, matcher.end());
                    inComment = false;
                    commentStart = -1;
                }
                continue;
            }

            if (inComment) {
                continue;
            }

            String slash = matcher.group(4);
            String name = matcher.group(5);
            String rest = matcher.group(6);

            if (name == null) continue;

            String nameLower = name.toLowerCase();
            boolean selfClosing = rest != null && rest.trim().endsWith("/");

            if (slash.isEmpty() && !selfClosing && !VOID_ELEMENTS.contains(nameLower)) {
                // Opening tag
                stack.push(new Tag(name, matcher.start()));
            } else if (!slash.isEmpty()) {
                // Closing tag - pop until we find the matching open
                while (!stack.isEmpty()) {
                    Tag open = stack.pop();
                    if (open.name.equalsIgnoreCase(name)) {
                        addFoldRegion(open.startOffset, matcher.end());
                        break;
                    }
                }
            }
        }

        return foldRegions;
    }

    private static class Tag {
        final String name;
        final int startOffset;

        Tag(String name, int startOffset) {
            this.name = name;
            this.startOffset = startOffset;
        }
    }
}