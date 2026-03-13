package com.campaignworkbench.ide.editor.richtextfx.codeformatting;

/**
 * Interface for formatting source code.
 */
public interface ICodeFormatter {
    String format(String unformattedCode, int indent);
}
