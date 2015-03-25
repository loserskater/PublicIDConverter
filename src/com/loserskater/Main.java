/*
 * Copyright (c) 2015 Jason Maxfield
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.loserskater;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;

public class Main extends Application {

    private static final String VERSION = "0.1";

    private static final int SOURCE_XML = 0;
    private static final int PORT_XML = 1;
    private static final int SOURCE_SMALI = 2;
    private static final String DEFAULT_SEARCH_STRING = "0x7f";
    private static final String SOURCE_PUBLIC_XML_FILENAME = "sourcepublic";
    private static final String PORT_PUBLIC_XML_FILENAME = "portpublic";
    private static final String SOURCE_SMALI_FILENAME = "sourcesmali";

    private static final int CL_COMMAND_TYPE = 0;
    private static final int CL_OPTION = 1;
    private static final int CL_OPTION_VALUE = 2;
    private static final int CL_SOURCE_XML = 3;
    private static final int CL_SOURCE_SMALI = 4;
    private static final int CL_PORT_XML = 5;

    private String customSearchString = null;
    private String portXmlLabel = "Port public.xml:";
    private String sourceXmlLabel = "Source public.xml:";
    private String sourceFileLabel = "Source smali file:";
    private FileChooser.ExtensionFilter xmlExtension = new FileChooser.ExtensionFilter("XML", "*.xml");
    private FileChooser.ExtensionFilter smaliExtension = new FileChooser.ExtensionFilter("smali", "*.smali");

    private ArrayList<String> originalIds = new ArrayList<String>();
    private ArrayList<String> nameType = new ArrayList<String>();

    TextField customSearchField = new TextField();

    private File sourcePublicXml;
    private File portPublicXml;
    private File sourceSmaliFile;

    private boolean isConverting = false;
    private boolean isCustomSearch = false;
    private boolean isCommandLine = false;

    final Text statusText = new Text();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage stage) throws Exception {

        final Parameters params = getParameters();
        final List<String> parameters = params.getRaw();
        Console console = System.console();
        if (console != null) {
            isCommandLine = true;
            //Running from command line so get parameters
            if (parameters != null) {
                if (!parameters.isEmpty()) {
                    if (parameters.get(CL_COMMAND_TYPE).equalsIgnoreCase("c") || parameters.get(CL_COMMAND_TYPE).equalsIgnoreCase("convert")) {
                        isConverting = true;
                    }
                    if (parameters.get(CL_OPTION).equalsIgnoreCase("-s")) {
                        isCustomSearch = true;
                        customSearchString = parameters.get(CL_OPTION_VALUE);
                    }
                    if (getParamSize(parameters)) {
                        loadParams(parameters);
                        if (parameters.get(CL_COMMAND_TYPE).equalsIgnoreCase("f") || parameters.get(CL_COMMAND_TYPE).equalsIgnoreCase("find")) {
                            cmdFind();
                        } else if (parameters.get(CL_COMMAND_TYPE).equalsIgnoreCase("c") || parameters.get(CL_COMMAND_TYPE).equalsIgnoreCase("convert")) {
                            cmdConvert();
                        } else {
                            showUsage();
                        }
                    } else {
                        showUsage();
                    }
                } else {
                    showUsage();
                }
                //Since we're running from command line we can exit here
                System.exit(0);
            }
        }

        stage.setTitle("Public ID Port");
        final BorderPane borderPane = new BorderPane();
        GridPane gridXML = new GridPane();
        setupGrid(gridXML);
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setHgrow(Priority.ALWAYS);
        column2.setMinWidth(400);
        gridXML.getColumnConstraints().addAll(new ColumnConstraints(), column2);

        getFilesFromPreference();

        Label sourceLabel = new Label(sourceXmlLabel);
        Label portLabel = new Label(portXmlLabel);
        Label fileLabel = new Label(sourceFileLabel);
        Label customValueLabel = new Label("Search string:");

        final TextField sourceTextField = new TextField();
        sourceTextField.setDisable(true);
        if (sourcePublicXml != null) {
            sourceTextField.setText(sourcePublicXml.getAbsolutePath());
        }

        final TextField portTextField = new TextField();
        portTextField.setDisable(true);
        if (portPublicXml != null) {
            portTextField.setText(portPublicXml.getAbsolutePath());
        }

        final TextField fileTextField = new TextField();
        fileTextField.setDisable(true);
        if (sourceSmaliFile != null) {
            fileTextField.setText(sourceSmaliFile.getAbsolutePath());
        }

        if (customSearchString == null) {
            customSearchField.setText(DEFAULT_SEARCH_STRING);
        } else {
            customSearchField.setText(customSearchString);
        }

        String open = "Open..";
        Button sourceButton = new Button(open);
        Button portButton = new Button(open);
        Button fileButton = new Button(open);
        Button convertButton = new Button("Convert");
        Button findButton = new Button("Find IDs");

        sourceButton.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                        openFile(sourceXmlLabel, stage, sourceTextField, xmlExtension, SOURCE_XML);
                    }
                }
        );

        portButton.setOnAction(
                new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(final ActionEvent e) {
                        openFile(portXmlLabel, stage, portTextField, xmlExtension, PORT_XML);
                    }
                }
        );

        convertButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                cmdConvert();
            }
        });

        findButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                cmdFind();
            }
        });


        fileButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                openFile(sourceFileLabel, stage, fileTextField, smaliExtension, SOURCE_SMALI);
            }
        });

        gridXML.add(sourceLabel, 0, 0);
        gridXML.add(sourceTextField, 1, 0);
        gridXML.add(sourceButton, 2, 0);

        gridXML.add(fileLabel, 0, 1);
        gridXML.add(fileTextField, 1, 1);
        gridXML.add(fileButton, 2, 1);

        HBox findBtn = new HBox(10);
        findBtn.setAlignment(Pos.CENTER);
        findBtn.getChildren().add(findButton);
        gridXML.add(findBtn, 1, 2);

        //Create an empty row
        gridXML.add(new Label(""), 0, 3);

        gridXML.add(portLabel, 0, 4);
        gridXML.add(portTextField, 1, 4);
        gridXML.add(portButton, 2, 4);

        HBox convertBtn = new HBox(10);
        convertBtn.setAlignment(Pos.CENTER);
        convertBtn.getChildren().add(convertButton);
        gridXML.add(convertBtn, 1, 5);

        GridPane optionsGrid = new GridPane();
        setupGrid(optionsGrid);

        Label optionsTitle = new Label("OPTIONS");
        optionsTitle.setFont(Font.font(null, FontWeight.BOLD, 16));
        optionsGrid.add(optionsTitle, 1, 0);

        optionsGrid.add(customValueLabel, 0, 1);
        optionsGrid.add(customSearchField, 1, 1);

        final Pane rootGroup = new VBox(12);
        rootGroup.getChildren().addAll(gridXML, optionsGrid);
        rootGroup.setPadding(new Insets(20));


        VBox statusBar = new VBox();
        statusBar.setStyle("-fx-background-color: #e9e9e9");
        statusBar.getChildren().add(statusText);

        borderPane.setCenter(rootGroup);
        borderPane.setBottom(statusBar);

        Scene scene = new Scene(borderPane);
        stage.setScene(scene);
        stage.show();
    }

    private boolean getParamSize(List<String> parameters) {
        int maxSize = 6;
        maxSize -= isCustomSearch ? 0 : 2;
        maxSize -= isConverting ? 0 : 1;
        return parameters.size() == maxSize;
    }

    private void showUsage() {
        System.out.println(
                "usage: public_id_convert f[ind] [options] <source public.xml> <source smali>" + "\n" +
                        "-s\t\t" + "The string that is searched for (default is 0x7f)" +
                        "public_id_convert c[onvert] [options] <source public.xml> <source smali> <port public.xml>" +
                        "-s\t\t" + "The string that is searched for (default is 0x7f)"
        );
    }

    private int getSourceXmlIndex() {
        return isCustomSearch ? CL_SOURCE_XML : CL_SOURCE_XML - 2;
    }

    private int getSourceSmaliIndex() {
        return isCustomSearch ? CL_SOURCE_SMALI : CL_SOURCE_SMALI - 2;
    }

    private int getPortXmlIndex() {
        return isCustomSearch ? CL_PORT_XML : CL_PORT_XML - 2;
    }

    private void loadParams(List<String> params) {
        sourcePublicXml = new File(params.get(getSourceXmlIndex()));
        sourceSmaliFile = new File(params.get(getSourceSmaliIndex()));
        if (isConverting) {
            portPublicXml = new File(params.get(getPortXmlIndex()));
        }

    }

    private void cmdConvert() {
        if (findIds()) {
            convertFile();
        }
    }

    private void updateStatusBar(String string) {
        statusText.setText(string);
        System.out.print(string + "\n");
    }

    private void cmdFind() {
        if (findIds() && nameType != null && !nameType.isEmpty()) {
            File tmp = new File("found_public_ids.txt");
            BufferedWriter writer = null;

            try {
                writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(tmp), "utf-8"));
                writer.write("This file is located at: ");
                writer.newLine();
                writer.write(tmp.getAbsolutePath());
                writer.newLine();
                writer.newLine();
                writer.write("Found the following " + nameType.size() + " IDs:");
                writer.newLine();
                for (int i = 0; i < nameType.size(); i++) {
                    writer.write(nameType.get(i) + " id=\"" + originalIds.get(i) + "\"");
                    writer.newLine();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            try {
                Desktop.getDesktop().edit(tmp);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    private void getFilesFromPreference() {
        sourcePublicXml = getFilePreference(SOURCE_PUBLIC_XML_FILENAME);
        portPublicXml = getFilePreference(PORT_PUBLIC_XML_FILENAME);
        sourceSmaliFile = getFilePreference(SOURCE_SMALI_FILENAME);
    }

    private boolean findIds() {
        if (!checkFiles()) {
            return false;
        }
        originalIds.clear();
        nameType.clear();

        if (!isCommandLine) {
            String searchString = customSearchField.getText().trim();
            customSearchString = searchString.matches(DEFAULT_SEARCH_STRING) ? null : searchString;
        }

        Scanner scanner = null;
        //Read source smali
        try {
            updateStatusBar("Reading smali...");
            scanner = new Scanner(sourceSmaliFile);
            while (scanner.hasNextLine()) {
                final String lineFromFile = scanner.nextLine();
                if (lineFromFile.contains(getSearchString())) {
                    int index = lineFromFile.indexOf(getSearchString());
                    String publicId = lineFromFile.substring(index, lineFromFile.length());
                    if (publicId.length() >= 6) {
                        originalIds.add(publicId);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                try {
                    scanner.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (originalIds.isEmpty()) {
            updateStatusBar("No ids found in smali file");
            return false;
        }

        //Search source public.xml
        try {
            updateStatusBar("Searching source public.xml for ids...");
            for (String id : originalIds) {
                boolean contains = false;
                scanner = new Scanner(sourcePublicXml);
                while (scanner.hasNextLine()) {
                    final String lineFromFile = scanner.nextLine();
                    if (lineFromFile.contains(id)) {
                        String name = lineFromFile.substring(lineFromFile.indexOf("type="), lineFromFile.indexOf("id=") - 1);
                        nameType.add(name);
                        contains = true;
                    }
                }
                if (!contains) {
                    nameType.add("Not found in source public.xml");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                try {
                    scanner.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (nameType.isEmpty()) {
            updateStatusBar("Could not find any ids in the source public.xml");
            return false;
        } else {
            updateStatusBar("Found " + nameType.size() + " IDs");
        }

        return true;
    }

    private void convertFile() {
        ArrayList<String> newIds = new ArrayList<String>();

        Scanner scanner = null;
        //Search port public.xml
        boolean empty = true;
        boolean missing = false;
        try {
            updateStatusBar("Search port public.xml for matching ids...");
            for (String aNameType : nameType) {
                boolean contains = false;
                scanner = new Scanner(portPublicXml);
                while (scanner.hasNextLine()) {
                    final String lineFromFile = scanner.nextLine();
                    if (lineFromFile.contains(aNameType)) {
                        String id = lineFromFile.substring(lineFromFile.indexOf("id=") + 4, lineFromFile.lastIndexOf(" ") - 1) + "    # " + aNameType;
                        newIds.add(id);
                        contains = true;
                        empty = false;
                    }
                }
                if (!contains) {
                    missing = true;
                    newIds.add("    # MISSING: " + aNameType);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                try {
                    scanner.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (empty) {
            updateStatusBar("No matching ids found in port public.xml");
            return;
        }

        //Search source file and change ids
        BufferedWriter writer = null;
        try {
            updateStatusBar("Replacing ids in smali file...");
            String filename = FilenameUtils.removeExtension(sourceSmaliFile.getName()) + "-MODIFIED" + FilenameUtils.getExtension(sourceSmaliFile.getName());
            File newFile = new File(FilenameUtils.getFullPath(sourceSmaliFile.getAbsolutePath()) + filename);
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(newFile)));
            scanner = new Scanner(sourceSmaliFile);
            while (scanner.hasNextLine()) {
                String lineFromFile = scanner.nextLine();
                for (int i = 0; i < originalIds.size(); i++) {
                    if (lineFromFile.contains(originalIds.get(i))) {
                        if (newIds.get(i).contains("MISSING")) {
                            lineFromFile += newIds.get(i);
                        } else {
                            lineFromFile = lineFromFile.replace(originalIds.get(i), newIds.get(i));
                        }
                        break;
                    }
                }
                writer.write(lineFromFile);
                writer.newLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (missing) {
            updateStatusBar("All done! But there were some missing ids. Search MODIFIED file for \"MISSING\"");
        } else {
            updateStatusBar("All done! New MODIFIED file will be in source smali directory.");
        }
    }

    private String getSearchString() {
        return customSearchString == null ? DEFAULT_SEARCH_STRING : customSearchString;
    }

    private boolean checkFiles() {
        if (sourcePublicXml == null || sourceSmaliFile == null) {
            if (isConverting && portPublicXml == null) {
                updateStatusBar("Please load all files!");
                return false;
            }
        }
        if (sourcePublicXml == null || !sourcePublicXml.exists()) {
            updateStatusBar("Could not open source public.xml");
            return false;
        }
        if (isConverting && (portPublicXml == null || !portPublicXml.exists())) {
            updateStatusBar("Could not open port public.xml");
            return false;
        }
        if (sourceSmaliFile == null || !sourceSmaliFile.exists()) {
            updateStatusBar("Could not open source smali");
            return false;
        }

        if (sourcePublicXml.isDirectory() || (isConverting && portPublicXml.isDirectory()) || sourceSmaliFile.isDirectory()) {
            updateStatusBar("Make sure you selected a file and not a directory");
            return false;
        }
        return true;
    }

    private void setupGrid(GridPane grid) {
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
    }

    private void openFile(String title, Stage stage, TextField textField, FileChooser.ExtensionFilter extension, int id) {
        final FileChooser fileChooser =
                new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(extension);
        if (getFile(id) != null) {
            fileChooser.setInitialDirectory(new File(FilenameUtils.getFullPath(getFile(id).getAbsolutePath())));
        }
        final File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            switch (id) {
                case SOURCE_XML:
                    sourcePublicXml = selectedFile;
                    setFilePreference(SOURCE_PUBLIC_XML_FILENAME, selectedFile);
                    break;
                case PORT_XML:
                    portPublicXml = selectedFile;
                    setFilePreference(PORT_PUBLIC_XML_FILENAME, selectedFile);
                    break;
                case SOURCE_SMALI:
                    sourceSmaliFile = selectedFile;
                    setFilePreference(SOURCE_SMALI_FILENAME, selectedFile);
                    break;
            }
            textField.setText(selectedFile.getAbsolutePath());
        }
    }

    private File getFile(int id) {
        switch (id) {
            case SOURCE_XML:
                return sourcePublicXml;
            case PORT_XML:
                return portPublicXml;
            case SOURCE_SMALI:
                return sourceSmaliFile;
            default:
                return null;
        }
    }

    public File getFilePreference(String filename) {
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        String filePath = prefs.get(filename, null);
        if (filePath != null) {
            return new File(filePath);
        } else {
            return null;
        }
    }

    public void setFilePreference(String filename, File file) {
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        if (file != null) {
            prefs.put(filename, file.getPath());
        } else {
            prefs.remove(filename);

        }
    }
}
