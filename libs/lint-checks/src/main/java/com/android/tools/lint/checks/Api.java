/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.lint.checks;

import com.android.annotations.NonNull;
import com.android.utils.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;

/**
 * Main entry point for API description.
 *
 * To create the {@link Api}, use {@link #parseApi(File)}
 *
 */
public class Api {
    /**
     * Parses simplified API file.
     * @param apiFile the file to read
     * @return a new ApiInfo
     */
    public static Api parseApi(File apiFile) {
        try (InputStream inputStream = new FileInputStream(apiFile)) {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            XmlUtils.configureSaxFactory(parserFactory, false, false);
            SAXParser parser = XmlUtils.createSaxParser(parserFactory);
            ApiParser apiParser = new ApiParser();
            parser.parse(inputStream, apiParser);
            inputStream.close();
            return new Api(apiParser.getClasses(), apiParser.getPackages());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private final Map<String, ApiClass> mClasses;
    private final Map<String, ApiPackage> mPackages;

    private Api(
            @NonNull Map<String, ApiClass> classes,
            @NonNull Map<String, ApiPackage> packages) {
        mClasses = new HashMap<>(classes);
        mPackages = new HashMap<>(packages);
    }

    ApiClass getClass(String fqcn) {
        return mClasses.get(fqcn);
    }

    Map<String, ApiClass> getClasses() {
        return Collections.unmodifiableMap(mClasses);
    }

    Map<String, ApiPackage> getPackages() {
        return Collections.unmodifiableMap(mPackages);
    }
}
