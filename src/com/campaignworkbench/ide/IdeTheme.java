package com.campaignworkbench.ide;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.NordLight;
import com.campaignworkbench.util.UiUtil;

import java.net.URL;

/**
 * Visual theme applicable to the entire IDE user interface
 */
public enum IdeTheme {

    DARK (
            new CupertinoDark().getUserAgentStylesheet(),
            "/styles/ide_styles_dark.css"
    ),

    LIGHT (
            new NordLight().getUserAgentStylesheet(),
            "/styles/ide_styles_light.css"
    );

    private final String atlantaFxStyleSheet;
    private final String ideStyleSheet;

    IdeTheme(String atlantaFxStyleSheet, String ideStyleSheet) {
        this.atlantaFxStyleSheet = atlantaFxStyleSheet;
        this.ideStyleSheet = ideStyleSheet;
    }

    public String getIdeStyleSheet() {
        return UiUtil.getStylesFromStyleSheet(ideStyleSheet);
    }
    public String getAtlantaFxStyleSheet() {
        return atlantaFxStyleSheet;
    }

}