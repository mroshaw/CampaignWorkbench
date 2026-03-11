package com.campaignworkbench.campaignrenderer;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Base class providing shared functionality for rendering template/module
 * content into executable JavaScript and running it in Rhino.
 */
public abstract class AbstractRenderer {

    protected String transformToJavaScript(String source) {
        StringBuilder js = new StringBuilder();
        js.append("var out = new java.lang.StringBuilder();\n");

        int pos = 0;
        while (pos < source.length()) {
            int start = source.indexOf("<%", pos);
            if (start == -1) {
                appendText(js, source.substring(pos));
                break;
            }

            appendText(js, source.substring(pos, start));

            int end = source.indexOf("%>", start);
            if (end == -1) {
                throw new IllegalArgumentException("Unclosed <% tag");
            }

            String code = source.substring(start + 2, end).trim();

            if (code.startsWith("=")) {
                js.append("out.append(")
                        .append(code.substring(1).trim())
                        .append(");\n");
            } else {
                js.append(code).append("\n");
            }

            pos = end + 2;
        }

        js.append("out.toString();");
        return js.toString();
    }

    protected void appendText(StringBuilder js, String text) {
        if (text.isEmpty()) return;
        text = text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "\\n");
        js.append("out.append(\"").append(text).append("\");\n");
    }

    /**
     * Wraps inner JavaScript with the standard boilerplate that creates `out`
     * and returns its contents at the end.
     */
    protected String buildJSWrapper(String innerJS) {
        return "var out = new java.lang.StringBuilder();\n" +
                innerJS +
                "out.toString();";
    }

    /**
     * Evaluates the given JS in Rhino and returns its string representation.
     */
    protected String evaluateToString(Context cx, Scriptable scope, String js, String sourceName) {
        Object result = cx.evaluateString(scope, js, sourceName, 1, null);
        return Context.toString(result);
    }
}
