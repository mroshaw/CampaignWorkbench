package com.campaignworkbench.ide.editor.richtextfx.syntaxhighlighting;

import com.campaignworkbench.ide.IdeTheme;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of ISyntaxStyler that provides syntax highlighting for Adobe Campaign
 * modules, blocks and templates. These are HTML documents with embedded JavaScript
 * scriptlets delimited by <% %>, <%= %> (expression), and <%@ %> (directive) tokens.
 */
public class CampaignSyntaxStyler implements ISyntaxStyler {

    boolean hasSpans = false;

    // --- ACC delimiters ---
    private static final String PAT_ACC_DIRECTIVE  = "<%@[^%]*+(?:%(?!>)[^%]*+)*+%>";
    private static final String PAT_ACC_EXPRESSION = "<%=[^%]*+(?:%(?!>)[^%]*+)*+%>";
    private static final String PAT_ACC_SCRIPTLET  = "<%[^%]*+(?:%(?!>)[^%]*+)*+%>";

    // --- ACC attribute access ---
    private static final String PAT_ACC_CTX_ATTR = "ctx\\.@[A-Z_a-z][A-Z_a-z0-9]*";
    private static final String PAT_ACC_RT_EVENT = "rtEvent\\.ctx(?:\\.[a-zA-Z_][a-zA-Z0-9_]*(?:\\[[0-9]+\\])?)*";

    // --- ACC global functions ---
    private static final String PAT_ACC_FUNCTION =
            "\\b(include|loadLibrary" +
                    "|logInfo|logError|logWarning|logVerbose" +
                    "|getUUID|formatDate|parseTimeStamp" +
                    "|escapeXmlStr|escapeUrl|getUrl" +
                    "|cryptString|decryptString|encryptDES|decryptDES" +
                    "|digestStrMd5|digestStrSha1|digestStrSha224|digestStrSha256|digestStrSha384|digestStrSha512" +
                    "|hmacStr)\\b";

    // --- ACC global objects / namespaces ---
    private static final String PAT_ACC_OBJECT =
            "\\b(NLWS|xtk|nms|vars|instance|task|activity|event|events|recipient|message|delivery|document|application)\\b";

    // --- HTML ---
    private static final String PAT_HTML_COMMENT           = "<!--(?:[^-]|-(?!-))*-->";
    private static final String PAT_HTML_TAG_OPEN          = "</?[a-zA-Z][a-zA-Z0-9]*(?=[ \\t\\n\\r/>])";
    private static final String PAT_HTML_TAG_CLOSE         = "/?>";
    private static final String PAT_HTML_ATTR_NAME         = "\\b[a-zA-Z_:][a-zA-Z0-9_:\\-.]*(?=\\s*=)";
    private static final String PAT_HTML_ATTR_VALUE_DOUBLE = "\"[^\"]*\"";
    private static final String PAT_HTML_ATTR_VALUE_SINGLE = "(?<==\\s?)'[^']*'";
    private static final String PAT_HTML_ENTITY            = "&[a-zA-Z][a-zA-Z0-9]*;|&#[0-9]+;|&#x[0-9a-fA-F]+;";

    // --- JavaScript ---
    private static final String PAT_JS_COMMENT_MULTI  = "/\\*(?:[^*]|\\*(?!/))*\\*/";
    private static final String PAT_JS_COMMENT        = "//[^\\n]*";
    private static final String PAT_JS_STRING         = "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'|`[^`\\\\]*(?:\\\\.[^`\\\\]*)*`";
    private static final String PAT_JS_KEYWORD        =
            "\\b(break|case|catch|continue|default|do|else|finally|for|if|return|switch|throw|try|while|with" +
                    "|var|function|const|let|class|extends|import|export|of" +
                    "|this|new|delete|typeof|instanceof|void|in|yield" +
                    "|true|false|null|undefined)\\b";
    private static final String PAT_JS_NUMBER         = "\\b0[xX][0-9a-fA-F]+\\b|\\b[0-9]+(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?\\b";

    // --- Master pattern (outer pass — HTML + ACC block detection) ---
    private static final Pattern MASTER = Pattern.compile(
            "(?<ACCDIRECTIVE>"        + PAT_ACC_DIRECTIVE          + ")" + "|" +
                    "(?<ACCEXPRESSION>"       + PAT_ACC_EXPRESSION         + ")" + "|" +
                    "(?<ACCSCRIPTLET>"        + PAT_ACC_SCRIPTLET          + ")" + "|" +
                    "(?<ACCCTXATTR>"          + PAT_ACC_CTX_ATTR           + ")" + "|" +
                    "(?<ACCRTEVENT>"          + PAT_ACC_RT_EVENT           + ")" + "|" +
                    "(?<HTMLCOMMENT>"         + PAT_HTML_COMMENT           + ")" + "|" +
                    "(?<HTMLTAGOPEN>"         + PAT_HTML_TAG_OPEN          + ")" + "|" +
                    "(?<HTMLTAGCLOSE>"        + PAT_HTML_TAG_CLOSE         + ")" + "|" +
                    "(?<HTMLATTRNAME>"        + PAT_HTML_ATTR_NAME         + ")" + "|" +
                    "(?<HTMLATTRVALUEDOUBLE>" + PAT_HTML_ATTR_VALUE_DOUBLE + ")" + "|" +
                    "(?<HTMLATTRVALUESINGLE>" + PAT_HTML_ATTR_VALUE_SINGLE + ")" + "|" +
                    "(?<HTMLENTITY>"          + PAT_HTML_ENTITY            + ")" + "|" +
                    "(?<JSCOMMENTMULTI>"      + PAT_JS_COMMENT_MULTI       + ")" + "|" +
                    "(?<JSCOMMENT>"           + PAT_JS_COMMENT             + ")" + "|" +
                    "(?<JSSTRING>"            + PAT_JS_STRING              + ")" + "|" +
                    "(?<ACCOBJECT>"           + PAT_ACC_OBJECT             + ")" + "|" +
                    "(?<ACCFUNCTION>"         + PAT_ACC_FUNCTION           + ")" + "|" +
                    "(?<JSKEYWORD>"           + PAT_JS_KEYWORD             + ")" + "|" +
                    "(?<JSNUMBER>"            + PAT_JS_NUMBER              + ")",
            Pattern.DOTALL
    );

    // --- Inner pattern (JS pass inside scriptlet/expression blocks) ---
    private static final Pattern JS_PATTERN = Pattern.compile(
            "(?<JSCOMMENTMULTI>" + PAT_JS_COMMENT_MULTI + ")" + "|" +
                    "(?<JSCOMMENT>"      + PAT_JS_COMMENT       + ")" + "|" +
                    "(?<JSSTRING>"       + PAT_JS_STRING        + ")" + "|" +
                    "(?<ACCOBJECT>"      + PAT_ACC_OBJECT       + ")" + "|" +
                    "(?<ACCFUNCTION>"    + PAT_ACC_FUNCTION     + ")" + "|" +
                    "(?<JSKEYWORD>"      + PAT_JS_KEYWORD       + ")" + "|" +
                    "(?<JSNUMBER>"       + PAT_JS_NUMBER        + ")",
            Pattern.DOTALL
    );

    // --- Directive sub-token pattern ---
    private static final Pattern DIRECTIVE_TOKENS = Pattern.compile(
            "(?<DIRKEYWORD>\\b(?:include|param|taglib)\\b)" + "|" +
                    "(?<DIRSTRING>\"[^\"]*\"|'[^']*')"
    );

    // --- Splits a scriptlet/expression/directive block into open delimiter, content, close delimiter ---
    private static final Pattern SCRIPTLET_DELIMITERS = Pattern.compile(
            "^(<%[@=]?)([^%]*+(?:%(?!>)[^%]*+)*+)(%>)$"
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

            String group = resolveGroup(m);
            if (group.equals("ACCSCRIPTLET") || group.equals("ACCEXPRESSION") || group.equals("ACCDIRECTIVE")) {
                appendScriptletSpans(builder, m.group());
            } else {
                builder.add(Collections.singleton(resolveStyleClass(group)), m.end() - m.start());
            }

            lastEnd = m.end();
        }

        if (lastEnd < text.length()) {
            builder.add(Collections.singleton("plain-text"), text.length() - lastEnd);
        }

        // Guard against empty builder
        if (!hasSpans) {
            builder.add(Collections.singleton("plain-text"), text.length());
        }
        return builder.create();
    }

    private void appendScriptletSpans(StyleSpansBuilder<Collection<String>> builder, String block) {
        Matcher delim = SCRIPTLET_DELIMITERS.matcher(block);
        if (!delim.matches()) {
            builder.add(Collections.singleton("acc-scriptlet"), block.length());
            return;
        }

        String open    = delim.group(1);
        String content = delim.group(2);
        String close   = delim.group(3);

        String delimStyle = open.equals("<%@") ? "acc-directive" :
                open.equals("<%=") ? "acc-expression" : "acc-scriptlet";

        builder.add(Collections.singleton(delimStyle), open.length());

        if (open.equals("<%@")) {
            // Sub-tokenise directive content for keyword and string values
            Matcher dt = DIRECTIVE_TOKENS.matcher(content);
            int lastDt = 0;
            while (dt.find()) {
                if (dt.start() > lastDt) {
                    builder.add(Collections.singleton("acc-directive"), dt.start() - lastDt);
                }
                String styleClass = dt.group("DIRKEYWORD") != null ? "acc-function" : "js-string";
                builder.add(Collections.singleton(styleClass), dt.end() - dt.start());
                hasSpans = true;
                lastDt = dt.end();
            }
            if (lastDt < content.length()) {
                builder.add(Collections.singleton("acc-directive"), content.length() - lastDt);
            }
        } else {
            // Scriptlet or expression — sub-tokenise as JavaScript
            Matcher js = JS_PATTERN.matcher(content);
            int lastJs = 0;
            while (js.find()) {
                if (js.start() > lastJs) {
                    builder.add(Collections.singleton("plain-text"), js.start() - lastJs);
                }
                builder.add(Collections.singleton(resolveJsStyleClass(js)), js.end() - js.start());
                lastJs = js.end();
            }
            if (lastJs < content.length()) {
                builder.add(Collections.singleton("plain-text"), content.length() - lastJs);
            }
        }

        builder.add(Collections.singleton(delimStyle), close.length());
    }

    private String resolveGroup(Matcher m) {
        if (m.group("ACCDIRECTIVE")        != null) return "ACCDIRECTIVE";
        if (m.group("ACCEXPRESSION")       != null) return "ACCEXPRESSION";
        if (m.group("ACCSCRIPTLET")        != null) return "ACCSCRIPTLET";
        if (m.group("ACCCTXATTR")          != null) return "ACCCTXATTR";
        if (m.group("ACCRTEVENT")          != null) return "ACCRTEVENT";
        if (m.group("HTMLCOMMENT")         != null) return "HTMLCOMMENT";
        if (m.group("HTMLTAGOPEN")         != null) return "HTMLTAGOPEN";
        if (m.group("HTMLTAGCLOSE")        != null) return "HTMLTAGCLOSE";
        if (m.group("HTMLATTRNAME")        != null) return "HTMLATTRNAME";
        if (m.group("HTMLATTRVALUEDOUBLE") != null) return "HTMLATTRVALUEDOUBLE";
        if (m.group("HTMLATTRVALUESINGLE") != null) return "HTMLATTRVALUESINGLE";
        if (m.group("HTMLENTITY")          != null) return "HTMLENTITY";
        if (m.group("JSCOMMENTMULTI")      != null) return "JSCOMMENTMULTI";
        if (m.group("JSCOMMENT")           != null) return "JSCOMMENT";
        if (m.group("JSSTRING")            != null) return "JSSTRING";
        if (m.group("ACCOBJECT")           != null) return "ACCOBJECT";
        if (m.group("ACCFUNCTION")         != null) return "ACCFUNCTION";
        if (m.group("JSKEYWORD")           != null) return "JSKEYWORD";
        if (m.group("JSNUMBER")            != null) return "JSNUMBER";
        return "";
    }

    private String resolveStyleClass(String group) {
        switch (group) {
            case "ACCDIRECTIVE":        return "acc-directive";
            case "ACCCTXATTR":          return "acc-ctx-attr";
            case "ACCRTEVENT":          return "acc-rt-event";
            case "HTMLCOMMENT":         return "html-comment";
            case "HTMLTAGOPEN":         return "html-tag";
            case "HTMLTAGCLOSE":        return "html-tag";
            case "HTMLATTRNAME":        return "html-attr-name";
            case "HTMLATTRVALUEDOUBLE": return "html-attr-value";
            case "HTMLATTRVALUESINGLE": return "html-attr-value";
            case "HTMLENTITY":          return "html-entity";
            case "JSCOMMENTMULTI":      return "js-comment";
            case "JSCOMMENT":           return "js-comment";
            case "JSSTRING":            return "js-string";
            case "ACCOBJECT":           return "acc-object";
            case "ACCFUNCTION":         return "acc-function";
            case "JSKEYWORD":           return "js-keyword";
            case "JSNUMBER":            return "js-number";
            default:                    return "plain-text";
        }
    }

    private String resolveJsStyleClass(Matcher m) {
        if (m.group("JSCOMMENTMULTI") != null) return "js-comment";
        if (m.group("JSCOMMENT")      != null) return "js-comment";
        if (m.group("JSSTRING")       != null) return "js-string";
        if (m.group("ACCOBJECT")      != null) return "acc-object";
        if (m.group("ACCFUNCTION")    != null) return "acc-function";
        if (m.group("JSKEYWORD")      != null) return "js-keyword";
        if (m.group("JSNUMBER")       != null) return "js-number";
        return "plain-text";
    }
    }