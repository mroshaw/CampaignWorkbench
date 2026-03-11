package com.campaignworkbench.ide.editor.richtextfx;
import com.campaignworkbench.ide.IdeException;

public class JavaScriptFormatter implements ICodeFormatter {

    @Override
    public String format(String unformattedCode, int indent) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < unformattedCode.length()) {
            int openToken = unformattedCode.indexOf("<%", i);

            if (openToken == -1) {
                result.append(unformattedCode.substring(i));
                break;
            }

            // Append everything before the opening token as-is
            String before = unformattedCode.substring(i, openToken);
            result.append(before);

            int closeToken = unformattedCode.indexOf("%>", openToken + indent);

            if (closeToken == -1) {
                result.append(unformattedCode.substring(openToken));
                break;
            }

            // Determine the indent level of the <% token by counting
            // the leading spaces on its line
            int lineStart = unformattedCode.lastIndexOf("\n", openToken);
            lineStart = (lineStart == -1) ? 0 : lineStart + 1;
            String leadingWhitespace = unformattedCode.substring(lineStart, openToken);
            int tokenIndentLevel = leadingWhitespace.length() / indent; // divide by spaces-per-indent

            // Extract the block between the tokens
            String blockContent = unformattedCode.substring(openToken + indent, closeToken);

            if (blockContent.stripLeading().startsWith("@")) {
                // Skip formatting for directive blocks
                result.append("<%");
                result.append(blockContent);
                result.append("%>");
            } else {
                result.append("<%");
                result.append(formatBlock(blockContent, tokenIndentLevel + 1));
                result.append("%>");
            }

            i = closeToken + 2;
        }

        return result.toString();
    }

    private String formatBlock(String unformattedCode, int indentLevel) {
        try {
            StringBuilder result = new StringBuilder();
            int i = 0;
            String[] lines = unformattedCode.split("\n");

            for (String line : lines) {
                String trimmed = line.stripLeading();

                if (trimmed.isEmpty()) {
                    result.append("\n");
                    continue;
                }

                // If the line starts with a closing brace, reduce indent before writing
                if (trimmed.startsWith("}")) {
                    indentLevel = Math.max(0, indentLevel - 1);
                }

                // Write the indented line
                result.append("  ".repeat(indentLevel)).append(trimmed).append("\n");

                // Count brace balance on this line, skipping strings and comments
                indentLevel += countBraceBalance(trimmed);
            }
            return result.toString();

        } catch (Exception evalException) {
            throw new IdeException("An error occurred formatting code: " + unformattedCode, evalException);
        }
    }

    private int countBraceBalance(String line) {
        int balance = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        int i = 0;

        while (i < line.length()) {
            char c = line.charAt(i);
            char next = (i + 1 < line.length()) ? line.charAt(i + 1) : 0;

            if (inLineComment) {
                // Rest of line is a comment, stop processing
                break;
            }

            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i += 2;
                    continue;
                }
            } else if (inSingleQuote) {
                if (c == '\\') {
                    i += 2; // skip escaped character
                    continue;
                }
                if (c == '\'') inSingleQuote = false;
            } else if (inDoubleQuote) {
                if (c == '\\') {
                    i += 2; // skip escaped character
                    continue;
                }
                if (c == '"') inDoubleQuote = false;
            } else {
                // Normal code
                if (c == '/' && next == '/') {
                    inLineComment = true;
                } else if (c == '/' && next == '*') {
                    inBlockComment = true;
                    i += 2;
                    continue;
                } else if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == '{') {
                    balance++;
                } else if (c == '}') {
                    balance--;
                }
            }
            i++;
        }

        return balance;
    }
}