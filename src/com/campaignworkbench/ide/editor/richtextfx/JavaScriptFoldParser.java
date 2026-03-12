package com.campaignworkbench.ide.editor.richtextfx;

import org.fxmisc.richtext.CodeArea;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of IFoldParser that identifies fold regions in JavaScript code.
 * Folds on matching brace pairs and multi-line block comments.
 */
public class JavaScriptFoldParser extends FoldParser implements IFoldParser {

    // Matches strings, single-line comments, multi-line comments, and braces,
    // in priority order so braces inside strings/comments are ignored.
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(?:\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")" +   // double-quoted string
                    "|(?:'[^'\\\\]*(?:\\\\.[^'\\\\]*)*')" +      // single-quoted string
                    "|(?:`[^`\\\\]*(?:\\\\.[^`\\\\]*)*`)" +      // template literal
                    "|(//[^\\n]*)" +                             // group 1: single-line comment (skip)
                    "|(/\\*)" +                                  // group 2: block comment open
                    "|(\\*/)" +                                  // group 3: block comment close
                    "|([{}])",                                   // group 4: brace
            Pattern.DOTALL
    );

    public JavaScriptFoldParser(CodeArea codeArea) {
        super(codeArea);
    }

    @Override
    public void updateFoldRegions() {

        String text = codeArea.getText();

        Matcher matcher = TOKEN_PATTERN.matcher(text);
        Deque<Integer> braceStack = new ArrayDeque<>();
        boolean inBlockComment = false;
        int commentStart = -1;

        while (matcher.find()) {

            if (matcher.group(1) != null) {
                // Single-line comment — skip
                continue;
            }

            if (matcher.group(2) != null) {
                // Block comment open /*
                if (!inBlockComment) {
                    inBlockComment = true;
                    commentStart = matcher.start();
                }
                continue;
            }

            if (matcher.group(3) != null) {
                // Block comment close */
                if (inBlockComment) {
                    addFoldRegion(commentStart, matcher.end());
                    inBlockComment = false;
                    commentStart = -1;
                }
                continue;
            }

            if (inBlockComment) {
                continue;
            }

            String brace = matcher.group(4);
            if (brace == null) continue;

            if (brace.equals("{")) {
                braceStack.push(matcher.start());
            } else {
                // }
                if (!braceStack.isEmpty()) {
                    int start = braceStack.pop();
                    addFoldRegion(start, matcher.end());
                }
            }
        }
    }
}