# Campaign Workbench

## Introduction

Campaign Workbench is a Java FX based desktop application for developing, managing and testing email template code for use in Adobe Campaign Classic v8.

![](.\images\CampaignWorkbenchMainUi.png)

## Core functions

The application provides a modern Integrated Development Environment for the development and management of core Adobe Campaign code objects:

- Manage email templates, JST code modules, personalization blocks, context XML in a central "Workspace".
- Manage and apply different data/payload, message and module contexts for scenario/segmentation testing
- Custom code editor with syntax highlighting, code folding and code formatting.
- Simulate Adobe Campaign rendering of email templates using ES6 compliant JavaScript interpreter (Apache Rhino).
- Error reporting with line numbers.
- Visibility of rendered HTML as well as HTML source and pre-processed JavaScript.
- Integration with Campaign Server, via secure APIs and Server-to-Server Oath:
- Create and synchronise objects with a Campaign server instance.
- Backup/version control changes with an ability to review and restore from previous versions.

## How to build

Build using IntelliJ IDEA 2025.3 and above.

## Dependencies

Built and tested using these dependencies:

- [Liberica 25.0.2 SDK full](https://bell-sw.com/pages/downloads/#jdk-25-lts) (includes JavaFX)

All dependencies are pulled from Maven Central:

- [RichTextFX](https://mvnrepository.com/artifact/org.fxmisc.richtext/richtextfx) - used for custom code editing, providing syntax highlighting and code folding.
- [Java Keyring](https://mvnrepository.com/artifact/com.github.javakeyring/java-keyring) - provides secure native OS credential storage and retrieval.
- [AtlantaFX](https://mvnrepository.com/artifact/io.github.mkpaz/atlantafx-base) - precompiled CSS theme styling for JavaFX components.
- [Rhino](https://mvnrepository.com/artifact/org.mozilla/rhino-all) - JavaScript engine that closely matches the functionality of the Adobe Campaign SpiderMonkey engine.
- [Jackson Data Format XML](https://mvnrepository.com/artifact/tools.jackson.dataformat/jackson-dataformat-xml) - used to serialize and deserialize Campaign server objects to Java class instances.
- [Jackson JAXRS: JSON](https://mvnrepository.com/artifact/tools.jackson.jaxrs/jackson-jaxrs-json-provider) - used to serialize and deserialize JSON local config.

