/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 *  under the License.
 *
 *******************************************************************************/
package com.blackducksoftware.tools.nrt.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import com.blackducksoftware.tools.nrt.config.NRTConfigurationManager;
import com.blackducksoftware.tools.nrt.config.NRTConstants;
import com.blackducksoftware.tools.nrt.model.ComponentModel;
import com.blackducksoftware.tools.nrt.model.LicenseModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Generator for: - an HTML Report from a provided component map. - a Text based
 * report from provided map (user must enable in config)
 * 
 * @author akamen
 * 
 */
public class NRTReportGenerator {

    final private Logger log = Logger.getLogger(this.getClass().getName());

    private NRTConfigurationManager nrtConfig = null;

    private TreeMap<String, ComponentModel> componentMap = null;

    public NRTReportGenerator(NRTConfigurationManager nrtConfig,
            TreeMap<String, ComponentModel> compmap) {
        this.nrtConfig = nrtConfig;
        componentMap = compmap;
    }

    /**
     * Copies the HTML template into the finalHtmlOutput then injects the
     * generates JSON data into the specific div location and writes it out.
     * 
     * @param expectedFile
     */
    public void generateHTMLFromTemplate(File finalHtmlOutput) {

        log.info("Writing to report: " + finalHtmlOutput);
        String jsonComponentList = generateJSONFromObject(componentMap);
        String jsonPropertyList = generateJSONFromObject(nrtConfig
                .getOptionsForExport());
        // Construct a variable out of it
        jsonComponentList = "var compList=[" + jsonComponentList + "]";
        jsonPropertyList = "var propList=[" + jsonPropertyList + "]";

        PrintWriter writer = null;
        try {
            // Read the template
            Document doc = Jsoup.parse(finalHtmlOutput, "UTF-8");

            // Inject the JSON
            Elements jsonElementDivBlock = doc
                    .getElementsByClass(NRTConstants.HTML_JSON_DATA_BLOCK);

            // This will be empty, but it should exist
            Element jsonDivElement = jsonElementDivBlock.get(0);

            if (jsonDivElement != null) {
                // Remove any script tags from it, in case the user populated
                // the template incorrectly with data
                if (jsonDivElement.children().size() > 0) {
                    Elements children = jsonDivElement.children();
                    for (int i = 0; i < children.size(); i++) {
                        Element el = children.get(i);
                        el.remove();
                    }
                }

                addNewScriptElementWithJson(jsonDivElement, jsonComponentList);
                addNewScriptElementWithJson(jsonDivElement, jsonPropertyList);
            } else {
                log.error("Unable to find a valid critical DIV inside HTML template: "
                        + NRTConstants.HTML_JSON_DATA_BLOCK);
            }
            writer = new PrintWriter(finalHtmlOutput, "UTF-8");
            // Write out the file
            writer.write(doc.html());
            writer.flush();
            writer.close();

        } catch (Exception e) {
            log.error("Unable to write out final report file!", e);
        } finally {
            writer.close();
        }

    }

    private void addNewScriptElementWithJson(Element jsonCompListElement,
            String jsonText) {
        Element scriptElement = jsonCompListElement.appendElement("script");
        DataNode jsonNode = new DataNode(jsonText, "");
        scriptElement.appendChild(jsonNode);

    }

    /**
     * Writes JSON to file
     * 
     * @param outputFilename
     * @return Returns the String that was written out.
     */
    public String generateJSONFromObject(Object collection) {
        StringBuilder sb = new StringBuilder();

        try {
            Gson gson = new GsonBuilder().setPrettyPrinting()
                    .excludeFieldsWithoutExposeAnnotation().create();
            sb.append(gson.toJson(collection));
        } catch (Exception e) {
            log.error("Error while generating JSON", e);
        }

        return sb.toString();
    }

    /**
     * Generates Text output alongside the HTML
     * 
     * @param projectName
     * @param outputFilename
     * @throws Exception
     */
    public void generateTextReport(String projectName) throws Exception {

        PrintStream outputTextFile = null;
        FileOutputStream outputStream = null;

        try {
            File dir = new File(projectName + "_text_files\\");
            dir.mkdirs();
        } catch (SecurityException e) {
            log.error("Unable to create directory for file output", e);
        }

        for (String compKey : componentMap.keySet()) {
            ComponentModel model = componentMap.get(compKey);

            try {
                String name = model.getName() + "_" + model.getVersion();

                outputStream = new FileOutputStream(projectName
                        + "_text_files\\" + name + ".txt");

                outputTextFile = new PrintStream(outputStream);
            } catch (FileNotFoundException e) {
                outputStream.close();
                outputTextFile.close();
                log.error("File not found: " + e.getMessage());
            }

            // Add copyrights, filepaths and license text into the next column.
            /**
             * Write out the file paths
             */
            writeOutFilePaths(compKey, outputTextFile);

            /**
             * Write out all the copy rights
             */
            writeOutCopyrights(compKey, outputTextFile);

            /**
             * Write out all the licenses
             */
            writeOutLicenseText(compKey, outputTextFile);

        } // For all component names

        // Close the stream
        outputStream.close();
        outputTextFile.close();
    }

    private void writeOutLicenseText(String componentName,
            PrintStream outputTextFile) {
        try {
            outputTextFile.println();
            outputTextFile
                    .println("License texts ("
                            + (componentMap.get(componentName).getLicenses() != null ? componentMap
                                    .get(componentName).getLicenses().size()
                                    : "0") + ")");

            int licenseCounter = 0;
            if (componentMap.get(componentName).getLicenses() != null) {

                for (LicenseModel license : componentMap.get(componentName)
                        .getLicenseModels()) {

                    String licenseName = license.getName() != null ? license
                            .getName() + "(Taken from KnowledgeBase)"
                            : "license_" + licenseCounter
                                    + "(Taken from scanned file)";

                    if (nrtConfig.isTextFileOutput()) {
                        outputTextFile.println();
                        outputTextFile
                                .println("==========================================================================");
                        outputTextFile.println(licenseName);
                        outputTextFile.print(StringEscapeUtils
                                .unescapeHtml(Jsoup.clean(license.getText(),
                                        "", Whitelist.none(),
                                        new Document.OutputSettings()
                                                .prettyPrint(false))));
                    }
                    licenseCounter++;
                } // for all licenses
            } // if licenses exist

        } catch (Exception e) {
            log.error("Error writing out licenses", e);
        }
    }

    private void writeOutCopyrights(String componentName,
            PrintStream outputTextFile) {
        try {
            if (nrtConfig.isShowCopyrights()) {

                outputTextFile.println();
                outputTextFile
                        .println("copyrights ("
                                + (componentMap.get(componentName)
                                        .getCopyrights() != null ? componentMap
                                        .get(componentName).getCopyrights()
                                        .size() : "0") + ")");

                if (componentMap.get(componentName).getCopyrights() != null) {

                    for (String copyright : componentMap.get(componentName)
                            .getCopyrights()) {
                        outputTextFile.println(copyright);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unable to write out copyrights", e);
        }

    }

    private void writeOutFilePaths(String compKey, PrintStream outputTextFile) {

        if (nrtConfig.isShowFilePaths()) {
            try {
                outputTextFile
                        .println("file paths ("
                                + (componentMap.get(compKey).getPaths() != null ? componentMap
                                        .get(compKey).getPaths().size()
                                        : "0") + ")");
                Set<String> paths = componentMap.get(compKey).getPaths();
                if (paths != null) {
                    for (String path : componentMap.get(compKey).getPaths()) {
                        outputTextFile.println(path);
                    }
                } else {
                    log.info("No paths available for component key: " + compKey);
                }

            } // try
            catch (Exception e) {
                log.error("Unable to write out file paths", e);
            }
        } else {
            log.debug("Skipping paths section, set to false");
        }

    }
}
