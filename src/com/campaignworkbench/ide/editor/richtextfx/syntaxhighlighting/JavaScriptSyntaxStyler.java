package com.campaignworkbench.ide.editor.richtextfx.syntaxhighlighting;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of ISyntaxStyler that provides syntax highlighting for JavaScript code,
 * including Adobe Campaign reserved words, objects, and utility functions.
 */
public class JavaScriptSyntaxStyler implements ISyntaxStyler {

    // --- Core ECMAScript keywords ---
    private static final String PAT_JS_KEYWORD =
            "\\b(break|case|catch|continue|default|do|else|finally|for|if|return|switch|throw|try|while|with" +
                    "|var|function|const|let|class|extends|import|export|of" +
                    "|this|new|delete|typeof|instanceof|void|in|yield" +
                    "|true|false|null|undefined)\\b";

    // --- Adobe Campaign global functions ---
    private static final String PAT_ACC_FUNCTION =
            "\\b(include|loadLibrary" +
                    "|logInfo|logError|logWarning|logVerbose" +
                    "|getUUID|formatDate|parseTimeStamp" +
                    "|escapeXmlStr|escapeUrl|getUrl" +
                    "|cryptString|decryptString|encryptDES|decryptDES" +
                    "|digestStrMd5|digestStrSha1|digestStrSha224|digestStrSha256|digestStrSha384|digestStrSha512" +
                    "|hmacStr)\\b";

    // --- Adobe Campaign global objects / namespaces ---
    private static final String PAT_ACC_OBJECT =
            "\\b(NLWS|xtk|nms|vars|instance|task|activity|event|events|document|application)\\b";

    // --- Adobe Campaign ctx / rtEvent attribute access ---
    private static final String PAT_ACC_CTX_ATTR   = "ctx\\.@[A-Z_a-z][A-Z_a-z0-9]*";
    private static final String PAT_ACC_RT_EVENT    = "rtEvent\\.ctx(?:\\.[a-zA-Z_][a-zA-Z0-9_]*(?:\\[\\d+\\])?)*";

    // --- Strings, comments, numbers ---
    private static final String PAT_COMMENT_SINGLE  = "//[^\\n]*";
    private static final String PAT_COMMENT_MULTI = "/\\*(?:[^*]|\\*(?!/))*\\*/";
    private static final String PAT_STRING =
            "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'|`[^`\\\\]*(?:\\\\.[^`\\\\]*)*`";

    private static final String PAT_NUMBER =
            "\\b0[xX][0-9a-fA-F]+\\b|\\b[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?\\b";

    // --- Punctuation ---
    private static final String PAT_BRACE           = "[{}]";
    private static final String PAT_PAREN           = "[()]";
    private static final String PAT_BRACKET         = "[\\[\\]]";
    private static final String PAT_SEMICOLON       = ";";

    private static final Pattern MASTER = Pattern.compile(
            "(?<COMMENTMULTI>"  + PAT_COMMENT_MULTI   + ")" + "|" +
                    "(?<COMMENT>"       + PAT_COMMENT_SINGLE  + ")" + "|" +
                    "(?<STRING>"        + PAT_STRING          + ")" + "|" +
                    "(?<ACCCTXATTR>"    + PAT_ACC_CTX_ATTR    + ")" + "|" +
                    "(?<ACCRTEVENT>"    + PAT_ACC_RT_EVENT    + ")" + "|" +
                    "(?<ACCOBJECT>"     + PAT_ACC_OBJECT      + ")" + "|" +
                    "(?<ACCFUNCTION>"   + PAT_ACC_FUNCTION    + ")" + "|" +
                    "(?<KEYWORD>"       + PAT_JS_KEYWORD      + ")" + "|" +
                    "(?<NUMBER>"        + PAT_NUMBER          + ")" + "|" +
                    "(?<BRACE>"         + PAT_BRACE           + ")" + "|" +
                    "(?<PAREN>"         + PAT_PAREN           + ")" + "|" +
                    "(?<BRACKET>"       + PAT_BRACKET         + ")" + "|" +
                    "(?<SEMICOLON>"     + PAT_SEMICOLON       + ")"
    );

    @Override
    public StyleSpans<Collection<String>> style(String text) {
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        Matcher m = MASTER.matcher(text);
        int lastEnd = 0;

        while (m.find()) {
            if (m.start() > lastEnd) {
                builder.add(Collections.singleton("plain-text"), m.start() - lastEnd);
            }
            builder.add(Collections.singleton(resolveStyleClass(m)), m.end() - m.start());
            lastEnd = m.end();
        }

        if (lastEnd < text.length()) {
            builder.add(Collections.singleton("plain-text"), text.length() - lastEnd);
        }

        return builder.create();
    }

    private String resolveStyleClass(Matcher m) {
        if (m.group("COMMENTMULTI") != null) return "js-comment";
        if (m.group("COMMENT")      != null) return "js-comment";
        if (m.group("STRING")       != null) return "js-string";
        if (m.group("ACCCTXATTR")   != null) return "acc-ctx-attr";
        if (m.group("ACCRTEVENT")   != null) return "acc-rt-event";
        if (m.group("ACCOBJECT")    != null) return "acc-object";
        if (m.group("ACCFUNCTION")  != null) return "acc-function";
        if (m.group("KEYWORD")      != null) return "js-keyword";
        if (m.group("NUMBER")       != null) return "js-number";
        if (m.group("BRACE")        != null) return "js-brace";
        if (m.group("PAREN")        != null) return "js-paren";
        if (m.group("BRACKET")      != null) return "js-bracket";
        if (m.group("SEMICOLON")    != null) return "js-semicolon";
        return "plain-text";
    }
}