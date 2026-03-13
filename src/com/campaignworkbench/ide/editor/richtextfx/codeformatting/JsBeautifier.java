package com.campaignworkbench.ide.editor.richtextfx.codeformatting;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Utility class that uses Rhino and js-beautify to format JavaScript code.
 */
public class JsBeautifier {

    private static final String BEAUTIFY_JS = "/js/beautify.js";
    private static final ScriptableObject SHARED_SCOPE;

    static {
        Context cx = Context.enter();
        try {
            cx.setOptimizationLevel(-1);
            SHARED_SCOPE = cx.initSafeStandardObjects();
            cx.evaluateString(SHARED_SCOPE, "var module = { exports: {} }; var exports = module.exports;", "shim", 1, null);
            try (InputStream is = JsBeautifier.class.getResourceAsStream(BEAUTIFY_JS);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                cx.evaluateReader(SHARED_SCOPE, reader, "beautify.js", 1, null);
            }
            cx.evaluateString(SHARED_SCOPE, "var js_beautify = module.exports.js_beautify;", "init", 1, null);
            SHARED_SCOPE.sealObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise JsBeautifier", e);
        } finally {
            Context.exit();
        }
    }

    public static String beautify(String code, int indentSize) {
        return beautify(code, indentSize, 0);
    }

    public static String beautify(String code, int indentSize, int indentLevel) {
        Context cx = Context.enter();
        try {
            cx.setOptimizationLevel(-1);
            Scriptable scope = cx.newObject(SHARED_SCOPE);
            scope.setPrototype(SHARED_SCOPE);
            scope.setParentScope(null);

            String options = "{ indent_size: " + indentSize + ", indent_char: ' ', indent_level: " + indentLevel + " }";
            scope.put("__code__", scope, code);
            String script = "js_beautify(__code__, " + options + ");";
            Object result = cx.evaluateString(scope, script, "format", 1, null);

            return Context.toString(result);

        } catch (Exception e) {
            throw new RuntimeException("Failed to beautify JavaScript", e);
        } finally {
            Context.exit();
        }
    }
}