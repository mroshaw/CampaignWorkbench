package com.campaignworkbench.ide.editor.richtextfx.codeformatting;

/**
 * Implements an ICodeFormatter for Adobe Campaign template code.
 * Formats JavaScript inside multi-line <% %> blocks, leaving HTML
 * and single-line blocks untouched.
 */
public class CampaignFormatter implements ICodeFormatter {

    @Override
    public String format(String code, int indent) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < code.length()) {
            int openToken = code.indexOf("<%", i);

            if (openToken == -1) {
                // No more tokens - append the rest as-is
                result.append(code.substring(i));
                break;
            }

            // Append everything before the opening token as-is
            result.append(code, i, openToken);

            int closeToken = code.indexOf("%>", openToken + 2);

            if (closeToken == -1) {
                // No closing token - append the rest as-is
                result.append(code.substring(openToken));
                break;
            }

            String blockContent = code.substring(openToken + 2, closeToken);

            if (blockContent.startsWith("@") || blockContent.startsWith("=")) {
                // Directive <%@ %> or expression <%= %> - leave untouched
                result.append("<%");
                result.append(blockContent);
                result.append("%>");
            } else if (!blockContent.contains("\n")) {
                // Single-line script block - leave untouched
                result.append("<%");
                result.append(blockContent);
                result.append("%>");
            } else {
                // Calculate the indent level of the <% token from its leading whitespace
                int lineStart = code.lastIndexOf("\n", openToken);
                lineStart = (lineStart == -1) ? 0 : lineStart + 1;
                String leadingWhitespace = code.substring(lineStart, openToken);
                int tokenIndentLevel = leadingWhitespace.length() / indent;

                // Format with base indent level of tokenIndentLevel + 1
                String formatted = JsBeautifier.beautify(blockContent, indent, tokenIndentLevel + 1);
                result.append("<%").append("\n").append(formatted).append("\n").append(leadingWhitespace).append("%>");
            }

            i = closeToken + 2;
        }

        return result.toString();
    }
}