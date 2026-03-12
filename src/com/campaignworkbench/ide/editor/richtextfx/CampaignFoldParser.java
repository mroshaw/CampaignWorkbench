package com.campaignworkbench.ide.editor.richtextfx;

import org.fxmisc.richtext.CodeArea;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An implementation of IFoldParser that identifies fold regions in Adobe Campaign template code
 */
public class CampaignFoldParser extends FoldParser implements IFoldParser {

    // Matches {, }, <% or %>
    private static final Pattern DELIM_PATTERN = Pattern.compile(
            "(<%)" +                                          // group 1: script open
                    "|(%>)" +                                         // group 2: script close
                    "|(\\{)" +                                        // group 3: brace open
                    "|(\\})" +                                        // group 4: brace close
                    "|<(/?)([a-zA-Z][a-zA-Z0-9_:-]*)((?:[^>])*?)>",  // groups 5,6,7: HTML tag
            Pattern.DOTALL
    );
    public CampaignFoldParser(CodeArea codeArea) {
        super(codeArea);
    }

    @Override
    public void updateFoldRegions() {
        String text = codeArea.getText();

        Deque<Integer> braceStack = new ArrayDeque<>();
        Deque<Integer> tagStack = new ArrayDeque<>();
        Deque<HtmlFoldParser.Tag> htmlStack = new ArrayDeque<>();
        boolean inScriptBlock = false;

        Matcher matcher = DELIM_PATTERN.matcher(text);

        while (matcher.find()) {

            if (matcher.group(1) != null) {
                // <% - only track if multi-line (i.e. there's a newline before the closing %>)
                int nextClose = text.indexOf("%>", matcher.end());
                if (nextClose != -1 && text.substring(matcher.end(), nextClose).contains("\n")) {
                    tagStack.push(matcher.start());
                    inScriptBlock = true;
                }
                continue;
            }

            if (matcher.group(2) != null) {
                // %> - script close
                if (!tagStack.isEmpty()) {
                    int start = tagStack.pop();
                    addFoldRegion(start, matcher.end());
                }
                inScriptBlock = false;
                continue;
            }

            if (matcher.group(3) != null) {
                // { - only track inside script blocks
                if (inScriptBlock) {
                    braceStack.push(matcher.start());
                }
                continue;
            }

            if (matcher.group(4) != null) {
                // } - only track inside script blocks
                if (inScriptBlock && !braceStack.isEmpty()) {
                    int start = braceStack.pop();
                    addFoldRegion(start, matcher.end());
                }
                continue;
            }

            // HTML tag - only process outside script blocks
            if (!inScriptBlock) {
                String slash = matcher.group(5);
                String name = matcher.group(6);
                String rest = matcher.group(7);

                if (name == null) continue;

                boolean selfClosing = rest != null && rest.trim().endsWith("/");
                String nameLower = name.toLowerCase();

                if (slash.isEmpty() && !selfClosing && !HtmlFoldParser.VOID_ELEMENTS.contains(nameLower)) {
                    htmlStack.push(new HtmlFoldParser.Tag(name, matcher.start()));
                } else if (!slash.isEmpty()) {
                    while (!htmlStack.isEmpty()) {
                        HtmlFoldParser.Tag open = htmlStack.pop();
                        if (open.name.equalsIgnoreCase(name)) {
                            addFoldRegion(open.startOffset, matcher.end());
                            break;
                        }
                    }
                }
            }
        }
    }
}