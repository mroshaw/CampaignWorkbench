package com.campaignworkbench.ide.editor.richtextfx;

import com.campaignworkbench.ide.IdeTheme;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of ISyntaxStyler that provides syntax highlighting for HTML code
 */
public class HtmlSyntaxStyler implements ISyntaxStyler {

    private static final String PAT_HTML_COMMENT = "<!--(?:[^-]|-(?!-))*-->";
    private static final String PAT_HTML_ATTR_VALUE_SINGLE = "(?<==\\s?)'[^']*'";
    private static final String PAT_DOCTYPE         = "<!DOCTYPE[^>]*>";
    private static final String PAT_HTML_TAG_OPEN   = "</?[a-zA-Z][a-zA-Z0-9]*(?=[ \\t\\n\\r/>])";
    private static final String PAT_HTML_TAG_CLOSE  = "/?>";
    private static final String PAT_HTML_ATTR_NAME  = "\\b[a-zA-Z_:][a-zA-Z0-9_:\\-.]*(?=\\s*=)";
    private static final String PAT_HTML_ENTITY     = "&[a-zA-Z][a-zA-Z0-9]*;|&#[0-9]+;|&#x[0-9a-fA-F]+;";
    private static final String PAT_HTML_ATTR_VALUE_DOUBLE = "\"[^\"]*\"";

    private static final Pattern MASTER = Pattern.compile(
            "(?<HTMLCOMMENT>"   + PAT_HTML_COMMENT   + ")" + "|" +
                    "(?<DOCTYPE>"       + PAT_DOCTYPE        + ")" + "|" +
                    "(?<HTMLTAGOPEN>"   + PAT_HTML_TAG_OPEN  + ")" + "|" +
                    "(?<HTMLTAGCLOSE>"  + PAT_HTML_TAG_CLOSE + ")" + "|" +
                    "(?<HTMLATTRNAME>"  + PAT_HTML_ATTR_NAME + ")" + "|" +
                    "(?<HTMLATTRVALUEDOUBLE>" + PAT_HTML_ATTR_VALUE_DOUBLE + ")" + "|" +
                    "(?<HTMLATTRVALUESINGLE>" + PAT_HTML_ATTR_VALUE_SINGLE + ")" + "|" +
                    "(?<HTMLENTITY>"    + PAT_HTML_ENTITY    + ")"
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
        if (m.group("HTMLCOMMENT")   != null) return "html-comment";
        if (m.group("DOCTYPE")       != null) return "html-doctype";
        if (m.group("HTMLTAGOPEN")   != null) return "html-tag";
        if (m.group("HTMLTAGCLOSE")  != null) return "html-tag";
        if (m.group("HTMLATTRNAME")  != null) return "html-attr-name";
        if (m.group("HTMLATTRVALUEDOUBLE") != null) return "html-attr-value";
        if (m.group("HTMLATTRVALUESINGLE") != null) return "html-attr-value";
        if (m.group("HTMLENTITY")    != null) return "html-entity";
        return "plain-text";
    }

    @Override
    public String getStyleSheet(IdeTheme theme) {
        return theme.getHtmlSyntaxStyleSheet();
    }
}