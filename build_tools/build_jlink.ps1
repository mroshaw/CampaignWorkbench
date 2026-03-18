jlink `
--add-modules java.base,java.desktop,java.net.http,java.sql,java.scripting,java.xml,javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.web,jdk.crypto.ec,jdk.dynalink,jdk.security.auth,jdk.unsupported `
--output "$PSScriptRoot\..\runtime" `
--strip-debug `
--no-header-files `
--no-man-pages `
--compress=2
