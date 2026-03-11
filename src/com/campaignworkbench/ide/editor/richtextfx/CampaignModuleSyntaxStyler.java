package com.campaignworkbench.ide.editor.richtextfx;

import com.campaignworkbench.ide.IdeTheme;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CampaignModuleSyntaxStyler implements ISyntaxStyler {

    private static final String PAT_ACC_DIRECTIVE = "<%@[^%]*%>";
    private static final String PAT_ACC_EXPRESSION = "<%=.*?%>";
    private static final String PAT_ACC_SCRIPTLET = "<%.*?%>";
    private static final String PAT_ACC_CTX_ATTR = "ctx\\.@[A-Z_a-z][A-Z_a-z0-9]*";
    private static final String PAT_ACC_RT_EVENT = "rtEvent\\.ctx(?:\\.[a-zA-Z_][a-zA-Z0-9_]*(?:\\[[0-9]+\\])?)*";
    private static final String PAT_HTML_COMMENT = "<!--.*?-->";
    private static final String PAT_HTML_TAG_OPEN = "</?[a-zA-Z][a-zA-Z0-9]*(?=[ \\t\\n\\r>])";
    private static final String PAT_HTML_TAG_CLOSE = "/?>";
    private static final String PAT_HTML_ATTR_NAME = "\\b[a-zA-Z_:][a-zA-Z0-9_:\\-\\.]*(?=\\s*=)";
    private static final String PAT_HTML_ATTR_VALUE = "\"[^\"]*\"|'[^']*'";
    private static final String PAT_HTML_ENTITY = "&[a-zA-Z][a-zA-Z0-9]*;|&#[0-9]+;|&#x[0-9a-fA-F]+;";
    private static final String PAT_JS_KEYWORD = "\\b(var|let|const|function|if|else|for|each|in|return|new|true|false|null|undefined|this)\\b";
    private static final String PAT_JS_STRING = "\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'";
    private static final String PAT_JS_COMMENT = "//[^\\n]*";
    private static final String PAT_JS_COMMENT_MULTI = "/\\*.*?\\*/";
    private static final String PAT_JS_NUMBER = "\\b[0-9]+(\\.[0-9]+)?\\b";

    // Combined master pattern
    private static final Pattern MASTER = Pattern.compile(
            "(?<ACCDIRECTIVE>" + PAT_ACC_DIRECTIVE + ")" + "|" +
                    "(?<ACCEXPRESSION>" + PAT_ACC_EXPRESSION + ")" + "|" +
                    "(?<ACCSCRIPTLET>" + PAT_ACC_SCRIPTLET + ")" + "|" +
                    "(?<ACCCTXATTR>" + PAT_ACC_CTX_ATTR + ")" + "|" +
                    "(?<ACCRTEVENT>" + PAT_ACC_RT_EVENT + ")" + "|" +
                    "(?<HTMLCOMMENT>" + PAT_HTML_COMMENT + ")" + "|" +
                    "(?<HTMLTAGOPEN>" + PAT_HTML_TAG_OPEN + ")" + "|" +
                    "(?<HTMLTAGCLOSE>" + PAT_HTML_TAG_CLOSE + ")" + "|" +
                    "(?<HTMLATTRNAME>" + PAT_HTML_ATTR_NAME + ")" + "|" +
                    "(?<HTMLATTRVALUE>" + PAT_HTML_ATTR_VALUE + ")" + "|" +
                    "(?<HTMLENTITY>" + PAT_HTML_ENTITY + ")" + "|" +
                    "(?<JSKEYWORD>" + PAT_JS_KEYWORD + ")" + "|" +
                    "(?<JSSTRING>" + PAT_JS_STRING + ")" + "|" +
                    "(?<JSCOMMENT>" + PAT_JS_COMMENT + ")" + "|" +
                    "(?<JSCOMMENTMULTI>" + PAT_JS_COMMENT_MULTI + ")" + "|" +
                    "(?<JSNUMBER>" + PAT_JS_NUMBER + ")",
            Pattern.DOTALL
    );


    @Override
    public StyleSpans<Collection<String>> style(String text) {
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        Matcher m = MASTER.matcher(text);
        int lastEnd = 0;

        while (m.find()) {
            // Gap between last token and this one – plain text
            if (m.start() > lastEnd) {
                builder.add(Collections.singleton("plain-text"), m.start() - lastEnd);
            }

            String styleClass = resolveStyleClass(m);
            builder.add(Collections.singletonList(styleClass), m.end() - m.start());
            lastEnd = m.end();
        }

        // Trailing plain text
        if (lastEnd < text.length()) {
            builder.add(Collections.singleton("plain-text"), text.length() - lastEnd);
        }

        return builder.create();
    }

    private String resolveStyleClass(Matcher m) {
        if (m.group("ACCDIRECTIVE")    != null) return "acc-directive";
        if (m.group("ACCEXPRESSION")   != null) return "acc-expression";
        if (m.group("ACCSCRIPTLET")    != null) return "acc-scriptlet";
        if (m.group("ACCCTXATTR")      != null) return "acc-ctx-attr";
        if (m.group("ACCRTEVENT")      != null) return "acc-rt-event";
        if (m.group("HTMLCOMMENT")     != null) return "html-comment";
        if (m.group("HTMLTAGOPEN")     != null) return "html-tag";
        if (m.group("HTMLTAGCLOSE")    != null) return "html-tag";
        if (m.group("HTMLATTRNAME")    != null) return "html-attr-name";
        if (m.group("HTMLATTRVALUE")   != null) return "html-attr-value";
        if (m.group("HTMLENTITY")      != null) return "html-entity";
        if (m.group("JSKEYWORD")       != null) return "js-keyword";
        if (m.group("JSSTRING")        != null) return "js-string";
        if (m.group("JSCOMMENT")       != null) return "js-comment";
        if (m.group("JSCOMMENTMULTI")  != null) return "js-comment";
        if (m.group("JSNUMBER")        != null) return "js-number";
        return "text";
    }


    @Override
    public String getStyleSheet(IdeTheme theme) {
        return theme.getCampaignSyntaxStyleSheet();
    }
}
