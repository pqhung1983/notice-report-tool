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
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 *******************************************************************************/
package com.blackducksoftware.tools.nrt.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.blackducksoftware.tools.nrt.config.NRTConstants;
import com.blackducksoftware.tools.nrt.model.ComponentModel;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Tests the HTML generation with some of the configuration switches enabled.
 * 
 * @author akamen
 * 
 */
public class HtmlReportGeneratorConfigTest extends HtmlReportGeneratorSetup
{
    @ClassRule
    public static TemporaryFolder junitWorkingFolder = new TemporaryFolder();

    // Custom config file
    private static String configFile = "nrt_config_basic_protex.properties";

    // Soon to be populated HTML contents
    private static HtmlPage doc = null;

    @BeforeClass
    public static void setupFiles() throws IOException
    {
        String htmlTemplateStr = ClassLoader.getSystemResource(htmlTemplate).getFile();
        basicReportOutputLocation = junitWorkingFolder.newFile("int_test_with_flags_config.html");
        Files.copy(new File(htmlTemplateStr).toPath(), basicReportOutputLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);

        generator = setupFiles(configFile, basicReportOutputLocation);

        doc = getDocForBasicReport();
    }

    @Test
    public void testProjectNameInjection()
    {
        @SuppressWarnings("unchecked")
        List<HtmlDivision> compElements = (List<HtmlDivision>)
                doc.getByXPath(getXPathName(NRTConstants.HTML_TITLE_CLASS));
        List<HtmlElement> injectedProjectNameTag = compElements.get(0).getElementsByTagName("h1");

        Assert.assertEquals(1, injectedProjectNameTag.size());

        Map<String, Object> opts = configManager.getOptionsForExport();
        String expectedProjectName = (String) opts.get("project_name");
        List<DomNode> children = injectedProjectNameTag.get(0).getChildNodes();

        // There will be a text element, scripte element, another text element
        Assert.assertEquals(3, children.size());

        // Grab the third element, that is our injected project name
        Assert.assertEquals(expectedProjectName, children.get(2).getTextContent().trim());
    }

    /**
     * Test filepaths count
     * Because we disabled this feature, we expect zero
     * 
     * @throws IOException
     */
    @Test
    public void testFilePathsWhenSetToFalse() throws IOException
    {
        List<?> compElements = doc.getByXPath(getXPathName(NRTConstants.HTML_FILE_PATH_CLASS));

        Assert.assertEquals(0, compElements.size());
    }

    /**
     * Tests the user option to show versions inside the component name
     * 
     * @throws IOException
     */
    @Test
    public void testComponentNameWithVersion() throws IOException
    {
        @SuppressWarnings("unchecked")
        List<HtmlDivision> compElements = (List<HtmlDivision>) doc.getByXPath(getXPathName(NRTConstants.HTML_COMPONENT_CLASS));

        // Check if these elements exist, they certainly should.
        Assert.assertEquals(testComponents.size(), compElements.size());

        // Check name of first element
        ComponentModel compTestModel = testComponents.get(COMP_ONE_NAME);
        HtmlDivision firstElement = compElements.get(0);
        Assert.assertEquals(compTestModel.getName(), firstElement.getFirstChild().getTextContent());
    }
}
