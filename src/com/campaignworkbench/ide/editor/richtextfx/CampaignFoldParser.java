package com.campaignworkbench.ide.editor.richtextfx;

import org.fxmisc.richtext.CodeArea;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of IFoldParser that identifies fold regions in Adobe Campaign
 * template code — HTML documents with embedded JavaScript scriptlets delimited
 * by <% %>, <%= %>, and <%@ %>.
 *
 * Foldable regions:
 *   - HTML element open/close tag pairs (as HtmlFoldParser)
 *   - HTML comments <!-- -->
 *   - Scriptlet blocks <% %> that span multiple lines
 *   - JS brace pairs { } within scriptlet blocks
 */
public class CampaignFoldParser extends FoldParser implements IFoldParser {

    private String lastParsedText = null;

    private static final Set<String> VOID_ELEMENTS = Set.of(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
    );

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(<%(?:[^%]|%(?!>))*+%>)" +                                          // group 1:  scriptlet block
                    "|(<!--(?:(?!-->).)*-->)" +                                           // group 2:  single-line HTML comment
                    "|(<!--)" +                                                           // group 3:  HTML comment open
                    "|(-->)" +                                                            // group 4:  HTML comment close
                    "|(?:\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")" +                          // JS double-quoted string (no group)
                    "|(?:'[^'\\\\]*(?:\\\\.[^'\\\\]*)*')" +                              // JS single-quoted string (no group)
                    "|(?:`[^`\\\\]*(?:\\\\.[^`\\\\]*)*`)" +                              // JS template literal  (no group)
                    "|<(/?)([a-zA-Z][a-zA-Z0-9_:-]*)((?:<%(?:[^%]|%(?!>))*+%>|[^>])*?)>", // groups 5,6,7: HTML tag
            Pattern.DOTALL
    );

    private static final Pattern JS_BRACE_PATTERN = Pattern.compile(
            "(?:\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")" +
                    "|(?:'[^'\\\\]*(?:\\\\.[^'\\\\]*)*')" +
                    "|(?:`[^`\\\\]*(?:\\\\.[^`\\\\]*)*`)" +
                    "|(//[^\\n]*)" +                   // group 1: single-line comment
                    "|(/\\*)" +                        // group 2: block comment open
                    "|(\\*/)" +                        // group 3: block comment close
                    "|([{}])",                         // group 4: brace
            Pattern.DOTALL
    );

    public CampaignFoldParser(CodeArea codeArea) {
        super(codeArea);
    }

    @Override
    public FoldRegions findFoldRegions(CodeArea codeArea) {

        String text = codeArea.getText();

        if (text.equals(lastParsedText)) {
            return foldRegions; // already up to date
        }

        lastParsedText = text;
        foldRegions = new FoldRegions(codeArea);

        Matcher m = TOKEN_PATTERN.matcher(text);

        Deque<Tag> htmlStack = new ArrayDeque<>();

        boolean inHtmlComment = false;
        int htmlCommentStart = -1;

        while (m.find()) {

            // --- Scriptlet block <% ... %> ---
            if (m.group(1) != null) {
                debugAdd("SCRIPTLET", m.start(), m.end(), text);
                addRegion(m.start(), m.end());
                String scriptletContent = m.group(1);
                int contentStart = m.start() + (scriptletContent.startsWith("<%@") || scriptletContent.startsWith("<%=") ? 3 : 2);
                scanJsBraces(scriptletContent.substring(contentStart - m.start(), scriptletContent.length() - 2), contentStart, text);
                continue;
            }

            // --- Single-line HTML comment ---
            if (m.group(2) != null) {
                debugAdd("COMMENT-SINGLE", m.start(), m.end(), text);
                addRegion(m.start(), m.end());
                continue;
            }

            // --- HTML comment open ---
            if (m.group(3) != null) {
                if (!inHtmlComment) {
                    inHtmlComment = true;
                    htmlCommentStart = m.start();
                }
                continue;
            }

            // --- HTML comment close ---
            if (m.group(4) != null) {
                if (inHtmlComment) {
                    debugAdd("COMMENT-MULTI", htmlCommentStart, m.end(), text);
                    addRegion(htmlCommentStart, m.end());
                    inHtmlComment = false;
                    htmlCommentStart = -1;
                }
                continue;
            }

            if (inHtmlComment) continue;

            // --- HTML tag ---
            String slash = m.group(5);
            String name  = m.group(6);
            String rest  = m.group(7);

            if (name == null) continue;

            String nameLower = name.toLowerCase();
            boolean selfClosing = rest != null && rest.trim().endsWith("/");

            if (slash.isEmpty() && !selfClosing && !VOID_ELEMENTS.contains(nameLower)) {
                System.out.println("[FOLD] PUSH <" + name + "> at char " + m.start()
                        + " (para " + getParaIndex(m.start(), codeArea) + ")");
                htmlStack.push(new Tag(name, m.start()));
            } else if (!slash.isEmpty()) {
                Tag open = findAndRemoveNearest(htmlStack, name);
                if (open != null) {
                    debugAdd("HTML <" + open.name + ">...</" + name + ">", open.startOffset, m.end(), text);
                    addRegion(open.startOffset, m.end());
                }
            }
        }

        return foldRegions;
    }

    private Tag findAndRemoveNearest(Deque<Tag> stack, String tagName) {
        for (Iterator<Tag> it = stack.iterator(); it.hasNext();) {
            Tag t = it.next();
            if (t.name.equalsIgnoreCase(tagName)) {
                it.remove();
                return t;
            }
        }
        return null;
    }

    private void addRegion(int startChar, int endChar) {
        foldRegions.add(startChar, endChar, foldedParagraphs);
    }

    private void debugAdd(String label, int startChar, int endChar, String text) {
        int startPara = getParaIndex(startChar, null);
        int endPara   = getParaIndex(endChar,   null);
        String preview = text.substring(startChar, Math.min(startChar + 40, text.length()))
                .replace("\n", "↵").replace("\r", "");
        System.out.println("[FOLD] " + label
                + " | chars [" + startChar + "-" + endChar + "]"
                + " | paras [" + startPara + "-" + endPara + "]"
                + " | \"" + preview + "\"");
    }

    private int getParaIndex(int charOffset, CodeArea ca) {
        try {
            org.fxmisc.richtext.model.TwoDimensional.Position pos =
                    codeArea.offsetToPosition(charOffset, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward);
            return pos.getMajor();
        } catch (Exception e) {
            return -1;
        }
    }

    private void scanJsBraces(String scriptletContent, int scriptletAbsoluteStart, String fullText) {
        Matcher m = JS_BRACE_PATTERN.matcher(scriptletContent);
        Deque<Integer> braceStack = new ArrayDeque<>();
        boolean inBlockComment = false;
        int commentStart = -1;

        while (m.find()) {
            if (m.group(1) != null) continue;

            if (m.group(2) != null) {
                if (!inBlockComment) { inBlockComment = true; commentStart = m.start(); }
                continue;
            }
            if (m.group(3) != null) {
                if (inBlockComment) {
                    debugAdd("JS-BLOCK-COMMENT", scriptletAbsoluteStart + commentStart,
                            scriptletAbsoluteStart + m.end(), fullText);
                    addRegion(scriptletAbsoluteStart + commentStart,
                            scriptletAbsoluteStart + m.end());
                    inBlockComment = false;
                }
                continue;
            }
            if (inBlockComment) continue;

            String brace = m.group(4);
            if (brace == null) continue;

            if (brace.equals("{")) {
                braceStack.push(m.start());
            } else {
                if (!braceStack.isEmpty()) {
                    int braceStart = scriptletAbsoluteStart + braceStack.pop();
                    int braceEnd   = scriptletAbsoluteStart + m.end();
                    debugAdd("JS-BRACE", braceStart, braceEnd, fullText);
                    addRegion(braceStart, braceEnd);
                }
            }
        }
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