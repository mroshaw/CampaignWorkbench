package com.campaignworkbench.campaignrenderer;

import com.campaignworkbench.ide.IdeException;
import com.campaignworkbench.workspace.EtmModule;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.nio.file.Path;

/**
 * Renders a Module into template source.
 * A module is a "meta-template": its JavaScript produces template code.
 */
public final class ModuleRenderer extends AbstractRenderer {

    ModuleRenderer() {}

    public  String renderModule(
            EtmModule module,
            Context cx,
            Scriptable scope
    ) {

        // Get the module context
        if(!module.isDataContextSet())
        {
            throw new IdeException("Data context is not set on module: " + module.getFileName(), null);
        }

        Path xmlContextFile = module.getAbsoluteFilePath();
        String xmlContextContent = module.getDataContextContent();

        String moduleSource = module.getWorkspaceFileContent();
        String moduleFileName = module.getBaseFileName();
        String js = "";
        try {
            cx.evaluateString(
                    scope,
                    "var ctx = new XML(`" + xmlContextContent + "`);",
                    xmlContextFile.getFileName().toString(),
                    1,
                    null
            );

            js =
                    "var out = new java.lang.StringBuilder();\n" +
                            transformToJavaScript(moduleSource) +
                            "out.toString();";

            Object result = cx.evaluateString(scope, js, moduleFileName, 1, null);
            return Context.toString(result);
        }
        catch (org.mozilla.javascript.RhinoException rhinoException) {
            throw new RendererExecutionException(
                    "Error executing module: " + rhinoException.getMessage(),
                    module,
                    js,
                    rhinoException.lineNumber(),
                    rhinoException.details(),
                    "Check module source code for errors",
                    rhinoException
            );
        }
    }
}