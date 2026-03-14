package com.campaignworkbench.ide.themes;

/**
 * Interface for JavaFX components that support IDEThemes.
 */
public interface IThemeable {
    void applyTheme(IdeTheme oldTheme, IdeTheme newTheme);
}
