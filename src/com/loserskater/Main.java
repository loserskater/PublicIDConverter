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
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;

public class Main extends Application {

    private static final int SOURCE_XML = 0;
    private static final int PORT_XML = 1;
    private static final int SOURCE_SMALI = 2;
    private static final String SEARCH_STRING = "0x7f";
    private static final String SOURCE_PUBLIC_XML_FILENAME = "sourcepublic";
    private static final String PORT_PUBLIC_XML_FILENAME = "portpublic";
    private static final String SOURCE_SMALI_FILENAME = "sourcesmali";

    private String portXmlLabel = "Port public.xml:";
    private String sourceXmlLabel = "Source public.xml:";
    private String sourceFileLabel = "Source smali file:";
    private FileChooser.ExtensionFilter xmlExtension = new FileChooser.ExtensionFilter("XML", "*.xml");
    private FileChooser.ExtensionFilter smaliExtension = new FileChooser.ExtensionFilter("smali", "*.smali");

    private File sourcePublicXml;
    private File portPublicXml;
    private File sourceSmaliFile;

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
            if (parameters != null) {
                if (!parameters.isEmpty() && parameters.size() == 3) {
                    sourcePublicXml = new File(parameters.get(0));
                    portPublicXml = new File(parameters.get(1));
                    sourceSmaliFile = new File(parameters.get(2));
                    convertFile();
                } else {
                    String jarName = new File(Main.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .getPath())
                            .getName();
                    System.out.println(
                            "usage: " + FilenameUtils.removeExtension(jarName) + " <source public.xml> <port public.xml> <source smali>"
                    );
                }
                System.exit(0);
            }
        }

        stage.setTitle("Public ID Port");
        final BorderPane borderPane = new BorderPane();
        GridPane gridXML = new GridPane();
        setupGrid(gridXML);

        getFilesFromPreference();

        Label sourceLabel = new Label(sourceXmlLabel);
        Label portLabel = new Label(portXmlLabel);
        Label fileLabel = new Label(sourceFileLabel);

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

        String open = "Open..";
        Button sourceButton = new Button(open);
        Button portButton = new Button(open);
        Button fileButton = new Button(open);
        Button convert = new Button("Convert");

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

        convert.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                convertFile();
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

        gridXML.add(portLabel, 0, 1);
        gridXML.add(portTextField, 1, 1);
        gridXML.add(portButton, 2, 1);

        //Create an empty row
        gridXML.add(new Label(""), 0, 2);

        gridXML.add(fileLabel, 0, 3);
        gridXML.add(fileTextField, 1, 3);
        gridXML.add(fileButton, 2, 3);

        //Create an empty row
        gridXML.add(new Label(""), 0, 2);

        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.CENTER);
        hbBtn.getChildren().add(convert);
        gridXML.add(hbBtn, 1, 4);

        final Pane rootGroup = new VBox(12);
        rootGroup.getChildren().addAll(gridXML);
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

    private void updateStatusBar(String string) {
        statusText.setText(string);
        System.out.print(string + "\n");
    }

    private void getFilesFromPreference() {
        sourcePublicXml = getFilePreference(SOURCE_PUBLIC_XML_FILENAME);
        portPublicXml = getFilePreference(PORT_PUBLIC_XML_FILENAME);
        sourceSmaliFile = getFilePreference(SOURCE_SMALI_FILENAME);
    }

    private void convertFile() {
        if (!checkFiles()) {
            return;
        }

        ArrayList<String> ids = new ArrayList<String>();
        ArrayList<String> nameType = new ArrayList<String>();
        ArrayList<String> newIds = new ArrayList<String>();

        Scanner scanner = null;
        //Read source smali
        try {
            updateStatusBar("Reading smali...");
            scanner = new Scanner(sourceSmaliFile);
            while (scanner.hasNextLine()) {
                final String lineFromFile = scanner.nextLine();
                if (lineFromFile.contains(SEARCH_STRING)) {
                    int index = lineFromFile.indexOf(SEARCH_STRING);
                    String publicId = lineFromFile.substring(index, index + 10);
                    ids.add(publicId);
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

        if (ids.isEmpty()) {
            updateStatusBar("No public ids found in smali file");
            return;
        }

        //Search source public.xml
        try {
            updateStatusBar("Searching source public.xml for ids...");
            for (String id : ids) {
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
            updateStatusBar("Could not find ids in the source public.xml");
            return;
        }

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
        Writer writer = null;
        try {
            updateStatusBar("Replacing ids in smali file...");
            String filename = FilenameUtils.removeExtension(sourceSmaliFile.getName()) + "-MODIFIED" + FilenameUtils.getExtension(sourceSmaliFile.getName());
            File newFile = new File(FilenameUtils.getFullPath(sourceSmaliFile.getAbsolutePath()) + filename);
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(newFile)));
            scanner = new Scanner(sourceSmaliFile);
            while (scanner.hasNextLine()) {
                String lineFromFile = scanner.nextLine();
                for (int i = 0; i < ids.size(); i++) {
                    if (lineFromFile.contains(ids.get(i))) {
                        if (newIds.get(i).contains("MISSING")) {
                            lineFromFile += newIds.get(i);
                        } else {
                            lineFromFile = lineFromFile.replace(ids.get(i), newIds.get(i));
                        }
                        break;
                    }
                }
                writer.write(lineFromFile);
                writer.write("\n");
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

    private boolean checkFiles() {
        if (sourcePublicXml == null || portPublicXml == null || sourceSmaliFile == null) {
            updateStatusBar("Please load all files!");
            return false;
        }
        if (!sourcePublicXml.exists()) {
            updateStatusBar("Could not open source public.xml");
            return false;
        }
        if (!portPublicXml.exists()) {
            updateStatusBar("Could not open port public.xml");
            return false;
        }
        if (!sourceSmaliFile.exists()) {
            updateStatusBar("Could not open source smali");
            return false;
        }

        if (sourcePublicXml.isDirectory() || portPublicXml.isDirectory() || sourceSmaliFile.isDirectory()) {
            updateStatusBar("Make sure you selected a file and not a directory");
            return false;
        }
        return true;
    }

    private void setupGrid(GridPane grid) {
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ColumnConstraints column2 = new ColumnConstraints();
        column2.setHgrow(Priority.ALWAYS);
        column2.setMinWidth(400);
        grid.getColumnConstraints().addAll(new ColumnConstraints(), column2);
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
        switch (id){
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
