package com.campaignworkbench.test;

import com.campaignworkbench.ide.editor.richtextfx.codeformatting.JsBeautifier;

public class BeautifyTest {
    static void main(String[] args) {
        String testCode = "function greet(name) {\n" +
                "var message = \"Hello \" + name;\n" +
                "if (name === \"World\") {\n" +
                "message = \"Hello World\";\n" +
                "} else {\n" +
                "message = \"Hello \" + name;\n" +
                "}\n" +
                "return message;\n" +
                "}\n" +
                "\n" +
                "switch(day) {\n" +
                "case \"Monday\":\n" +
                "var x = 1;\n" +
                "break;\n" +
                "case \"Tuesday\":\n" +
                "var x = 2;\n" +
                "break;\n" +
                "default:\n" +
                "var x = 0;\n" +
                "}";

        String beautifiedCode = JsBeautifier.beautify(testCode, 2);
        System.out.println(beautifiedCode);
    }

}
