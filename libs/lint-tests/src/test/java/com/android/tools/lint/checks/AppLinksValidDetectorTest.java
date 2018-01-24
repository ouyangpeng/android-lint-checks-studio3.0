/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import com.android.tools.lint.checks.AppLinksValidDetector.UriInfo;
import com.android.tools.lint.detector.api.Detector;
import com.android.utils.XmlUtils;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@SuppressWarnings("javadoc")
public class AppLinksValidDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new AppLinksValidDetector();
    }

    public void testWrongNamespace() {
        String expected = ""
                + "AndroidManifest.xml:12: Error: Validation nodes should be in the tools: namespace to ensure they are removed from the manifest at build time [TestAppLink]\n"
                + "            <validation />\n"
                + "             ~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\""
                        + "    package=\"test.pkg\" >\n"
                        + "\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter android:autoVerify=\"true\">\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example.com\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "            </intent-filter>\n"
                        + "            <validation />\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testMissingTestUrl() {
        String expected = ""
                + "AndroidManifest.xml:12: Error: Expected testUrl attribute [AppLinkUrlError]\n"
                + "            <tools:validation />\n"
                + "            ~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\""
                        + "    package=\"test.pkg\" >\n"
                        + "\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter android:autoVerify=\"true\">\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example.com\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "            </intent-filter>\n"
                        + "            <tools:validation />\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testBadTestUrl() {
        String expected = ""
                + "AndroidManifest.xml:12: Error: Invalid test URL: no protocol: no-protocol [TestAppLink]\n"
                + "            <tools:validation testUrl=\"no-protocol\"/>\n"
                + "                                       ~~~~~~~~~~~\n"
                + "AndroidManifest.xml:13: Error: Invalid test URL: unknown protocol: unknown-protocol [TestAppLink]\n"
                + "            <tools:validation testUrl=\"unknown-protocol://example.com/gizmos/foo/bar\"/>\n"
                + "                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:14: Error: Invalid test URL: Invalid host: [FEDC:BA98:7654:3210:GEDC:BA98:7654:3210] [TestAppLink]\n"
                + "            <tools:validation testUrl=\"http://[FEDC:BA98:7654:3210:GEDC:BA98:7654:3210]:80/index.html\"/>\n"
                + "                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "3 errors, 0 warnings\n";
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\""
                        + "    package=\"test.pkg\" >\n"
                        + "\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter android:autoVerify=\"true\">\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example.com\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "            </intent-filter>\n"
                        + "            <tools:validation testUrl=\"no-protocol\"/>\n"
                        + "            <tools:validation testUrl=\"unknown-protocol://example.com/gizmos/foo/bar\"/>\n"
                        + "            <tools:validation testUrl=\"http://[FEDC:BA98:7654:3210:GEDC:BA98:7654:3210]:80/index.html\"/>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testValidation1() {
        String expected = ""
                + "AndroidManifest.xml:14: Error: Test URL did not match path prefix /gizmos, path literal /literal/path [TestAppLink]\n"
                + "            <tools:validation testUrl=\"http://example.com/notmatch/foo/bar\"/>\n"
                + "                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:15: Error: Test URL did not match host example.com [TestAppLink]\n"
                + "            <tools:validation testUrl=\"http://notmatch.com/gizmos/foo/bar\"/>\n"
                + "                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:16: Error: Test URL did not match scheme http [TestAppLink]\n"
                + "            <tools:validation testUrl=\"https://example.com/gizmos/foo/bar\"/>\n"
                + "                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "3 errors, 0 warnings\n";
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\""
                        + "    package=\"test.pkg\" >\n"
                        + "\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter android:autoVerify=\"true\">\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example.com\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "                <data android:path=\"/literal/path\" />\n"
                        + "            </intent-filter>\n"
                        + "            <tools:validation testUrl=\"http://example.com/gizmos/foo/bar\"/>\n"
                        + "            <tools:validation testUrl=\"http://example.com/notmatch/foo/bar\"/>\n"
                        + "            <tools:validation testUrl=\"http://notmatch.com/gizmos/foo/bar\"/>\n"
                        + "            <tools:validation testUrl=\"https://example.com/gizmos/foo/bar\"/>\n"
                        + "            <tools:validation testUrl=\"http://example.com/literal/path\"/>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testValidation2() {
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\""
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\" >\n"
                        + "        <activity android:name=\".MainActivity\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "                <data android:scheme=\"http\" />\n"
                        + "                <data android:scheme=\"https\" />\n"
                        + "                <data android:host=\"www.twitter.com\" />\n"
                        + "                <data android:host=\"twitter.com\" />\n"
                        + "                <data android:host=\"*.twitter.com\" />\n"
                        + "                <data android:host=\"*twitter.com\" />\n"
                        + "                <data android:pathPattern=\"/vioside/.*\" />\n"
                        + "            </intent-filter>\n"
                        + "            <tools:validation testUrl=\"https://twitter.com/vioside/status/761453456683069440\" />\n"
                        + "            <tools:validation testUrl=\"https://www.twitter.com/vioside/status/761453456683069440\" />\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expectClean();
    }

    public void testHostWildcardMatching() {
        String expected = ""
                + "AndroidManifest.xml:12: Error: Test URL did not match host *.example.com [TestAppLink]\n"
                + "            <tools:validation testUrl=\"http://example.com/path/foo/bar\"/>\n"
                + "                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\""
                        + "    package=\"test.pkg\" >\n"
                        + "\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter android:autoVerify=\"true\">\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"*.example.com\"\n"
                        + "                    android:pathPrefix=\"/path\" />\n"
                        + "            </intent-filter>\n"
                        // Not a match - missing "."
                        + "            <tools:validation testUrl=\"http://example.com/path/foo/bar\"/>\n"
                        // OK:
                        + "            <tools:validation testUrl=\"http://.example.com/path/foo/bar\"/>\n"
                        + "            <tools:validation testUrl=\"http://www.example.com/path/foo/bar\"/>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testPortMatching() {
        String expected = ""
                + "AndroidManifest.xml:25: Error: Test URL did not match port none or did not match port 85 or did not match host android.com [TestAppLink]\n"
                + "            <tools:validation testUrl=\"http://example.com:80/path/foo/bar\"/>\n"
                + "                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:29: Error: Test URL did not match host example.com or did not match port 86 [TestAppLink]\n"
                + "            <tools:validation testUrl=\"http://android.com/path/foo/bar\"/>\n"
                + "                                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n";
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\""
                        + "    package=\"test.pkg\" >\n"
                        + "\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter android:autoVerify=\"true\">\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                      android:host=\"example.com\"\n"
                        + "                      android:pathPrefix=\"/path\" />\n"
                        + "            </intent-filter>\n"
                        + "            <intent-filter android:autoVerify=\"true\">\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                      android:host=\"example.com\"\n"
                        + "                      android:port=\"85\"\n"
                        + "                      android:pathPrefix=\"/path\" />\n"
                        + "            </intent-filter>\n"
                        + "            <intent-filter android:autoVerify=\"true\">\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                      android:host=\"android.com\"\n"
                        + "                      android:port=\"86\"\n"
                        + "                      android:pathPrefix=\"/path\" />\n"
                        + "            </intent-filter>\n"
                        + "            <tools:validation testUrl=\"http://example.com/path/foo/bar\"/>\n"
                        + "            <tools:validation testUrl=\"http://example.com:80/path/foo/bar\"/>\n"
                        + "            <tools:validation testUrl=\"http://example.com/path/foo/bar\"/>\n"
                        + "            <tools:validation testUrl=\"http://example.com:85/path/foo/bar\"/>\n"
                        + "            <tools:validation testUrl=\"http://android.com:86/path/foo/bar\"/>\n"
                        + "            <tools:validation testUrl=\"http://android.com/path/foo/bar\"/>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testHostAndPortCombination() {
        // Host and port must be specified on the same element
        String expected = ""
                + "AndroidManifest.xml:12: Error: The port must be specified in the same <data> element as the host [AppLinkUrlError]\n"
                + "                <data android:port=\"80\" />\n"
                + "                                    ~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        // OK:
                        + "                <data android:scheme=\"http\"/>\n"
                        + "                <data android:host=\"example.com\"\n"
                        + "                      android:port=\"81\" />\n"
                        // Not OK:
                        + "                <data android:host=\"example.com\" />\n"
                        + "                <data android:port=\"80\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testValidPortNumber() {
        // Port numbers must be in the valid range
        String expected = ""
                + "AndroidManifest.xml:9: Error: not a valid port number [AppLinkUrlError]\n"
                + "                      android:port=\"-1\" />\n"
                + "                                    ~~\n"
                + "AndroidManifest.xml:11: Error: not a valid port number [AppLinkUrlError]\n"
                + "                      android:port=\"128000\" />\n"
                + "                                    ~~~~~~\n"
                + "2 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter>\n"
                        + "                <data android:scheme=\"http\"/>\n"
                        + "                <data android:host=\"example.com\"\n"
                        + "                      android:port=\"-1\" />\n"
                        + "                <data android:host=\"example.com\"\n"
                        + "                      android:port=\"128000\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testNonEmpty() {
        // Attributes are not allowed to be empty
        String expected = ""
                + "AndroidManifest.xml:7: Error: android:scheme cannot be empty [AppLinkUrlError]\n"
                + "                <data android:scheme=\"\"\n"
                + "                      ~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:8: Error: android:host cannot be empty [AppLinkUrlError]\n"
                + "                      android:host=\"\"\n"
                + "                      ~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:9: Error: android:port cannot be empty [AppLinkUrlError]\n"
                + "                      android:port=\"\"\n"
                + "                      ~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:10: Error: android:pathPrefix cannot be empty [AppLinkUrlError]\n"
                + "                      android:pathPrefix=\"\"\n"
                + "                      ~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:11: Error: android:path cannot be empty [AppLinkUrlError]\n"
                + "                      android:path=\"\"\n"
                + "                      ~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:12: Error: android:pathPattern cannot be empty [AppLinkUrlError]\n"
                + "                      android:pathPattern=\"\"\n"
                + "                      ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "6 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter>\n"
                        + "                <data android:scheme=\"\"\n"
                        + "                      android:host=\"\"\n"
                        + "                      android:port=\"\"\n"
                        + "                      android:pathPrefix=\"\"\n"
                        + "                      android:path=\"\"\n"
                        + "                      android:pathPattern=\"\"\n"
                        + "                      android:mimeType=\"\"/>\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testNoTrailingSchemeColon() {
        // There should be no trailing colons for schemes
        String expected = ""
                + "AndroidManifest.xml:7: Error: Don't include trailing colon in the scheme declaration [AppLinkUrlError]\n"
                + "                <data android:scheme=\"http:\"/>\n"
                + "                                      ~~~~~\n"
                + "AndroidManifest.xml:8: Error: Don't include trailing colon in the scheme declaration [AppLinkUrlError]\n"
                + "                <data android:scheme=\"https:\"/>\n"
                + "                                      ~~~~~~\n"
                + "2 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter>\n"
                        + "                <data android:scheme=\"http:\"/>\n"
                        + "                <data android:scheme=\"https:\"/>\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testWrongHostnameWildcard() {
        // Wildcard can only be at the beginning
        String expected = ""
                + "AndroidManifest.xml:8: Error: The host wildcard (*) can only be the first character [AppLinkUrlError]\n"
                + "                <data android:host=\"example.*.com\"\n"
                + "                                    ~~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter>\n"
                        + "                <data android:scheme=\"http\"/>\n"
                        + "                <data android:host=\"example.*.com\"\n />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testLowerCase() {
        // Scheme, host and mime type are all case sensitive and should only use lower case
        String expected = ""
                + "AndroidManifest.xml:8: Error: Scheme matching is case sensitive and should only use lower-case characters [AppLinkUrlError]\n"
                + "                <data android:scheme=\"HTTP\"\n"
                + "                                      ~~~~\n"
                + "AndroidManifest.xml:9: Error: Host matching is case sensitive and should only use lower-case characters [AppLinkUrlError]\n"
                + "                      android:host=\"Example.Com\"\n"
                + "                                    ~~~~~~~~~~~\n"
                + "AndroidManifest.xml:13: Error: Mime-type matching is case sensitive and should only use lower-case characters [AppLinkUrlError]\n"
                + "                      android:mimeType=\"MimeType\"/>\n"
                + "                                        ~~~~~~~~\n"
                + "3 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"HTTP\"\n"
                        + "                      android:host=\"Example.Com\"\n"
                        + "                      android:pathPrefix=\"/Foo\"\n"
                        + "                      android:path=\"/Foo\"\n"
                        + "                      android:pathPattern=\"/Foo\"\n"
                        + "                      android:mimeType=\"MimeType\"/>\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testPathsBeginWithSlash() {
        // Paths should begin with /
        String expected = ""
                + "AndroidManifest.xml:10: Error: android:pathPrefix attribute should start with /, but it is samplePrefix [AppLinkUrlError]\n"
                + "                      android:pathPrefix=\"samplePrefix\"\n"
                + "                                          ~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:11: Error: android:path attribute should start with /, but it is samplePath [AppLinkUrlError]\n"
                + "                      android:path=\"samplePath\"\n"
                + "                                    ~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"test.pkg\" >\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                      android:host=\"example.com\"\n"
                        + "                      android:pathPrefix=\"samplePrefix\"\n"
                        + "                      android:path=\"samplePath\"\n"
                        + "                      android:pathPattern=\"samplePattern\"/>\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for AndroidManifest.xml line 9: Replace with /samplePrefix:\n"
                        + "@@ -10 +10\n"
                        + "-                       android:pathPrefix=\"samplePrefix\"\n"
                        + "+                       android:pathPrefix=\"/samplePrefix\"\n"
                        + "Fix for AndroidManifest.xml line 10: Replace with /samplePath:\n"
                        + "@@ -11 +11\n"
                        + "-                       android:path=\"samplePath\"\n"
                        + "+                       android:path=\"/samplePath\"\n");
    }

    public void testSuppressWithOldId() {
        // Make sure that the ignore-issue mechanism works for both the current and the
        // previous issue id
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\""
                        + "    package=\"test.pkg\" >\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter>\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                      android:host=\"example.com\"\n"
                        + "                      android:pathPattern=\"foo\""
                        + "                      tools:ignore=\"AppLinkUrlError\"/>\n"
                        // Previous id
                        + "                <data android:scheme=\"http\"\n"
                        + "                      android:host=\"example.com\"\n"
                        + "                      android:pathPattern=\"foo\""
                        + "                      tools:ignore=\"GoogleAppIndexingUrlError\"/>\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expectClean();
    }


    public void testWrongPathPrefix() {
        String expected = ""
                + "AndroidManifest.xml:19: Error: android:pathPrefix attribute should start with /, but it is gizmos [AppLinkUrlError]\n"
                + "                    android:pathPrefix=\"gizmos\" />\n"
                + "                                        ~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".FullscreenActivity\"\n"
                        + "            android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "            android:label=\"@string/title_activity_fullscreen\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example.com\"\n"
                        + "                    android:pathPrefix=\"gizmos\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testWrongPort() {
        String expected = ""
                + "AndroidManifest.xml:19: Error: not a valid port number [AppLinkUrlError]\n"
                + "                    android:port=\"ABCD\"\n"
                + "                                  ~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".FullscreenActivity\"\n"
                        + "            android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "            android:label=\"@string/title_activity_fullscreen\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example.com\"\n"
                        + "                    android:port=\"ABCD\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testSchemeAndHostMissing() {
        String expected = ""
                + "AndroidManifest.xml:15: Error: Missing URL [AppLinkUrlError]\n"
                + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                + "            ^\n"
                + "AndroidManifest.xml:17: Error: At least one host must be specified [AppLinkUrlError]\n"
                + "                <data android:pathPrefix=\"/gizmos\" />\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:17: Error: At least one scheme must be specified [AppLinkUrlError]\n"
                + "                <data android:pathPrefix=\"/gizmos\" />\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:17: Error: Missing URL for the intent filter [AppLinkUrlError]\n"
                + "                <data android:pathPrefix=\"/gizmos\" />\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "4 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".FullscreenActivity\"\n"
                        + "            android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "            android:label=\"@string/title_activity_fullscreen\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:pathPrefix=\"/gizmos\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected)
                .verifyFixes().window(1)
                .expectFixDiffs(""
                        + "Fix for AndroidManifest.xml line 14: Set scheme=\"http\":\n"
                        + "@@ -15 +15\n"
                        + "              android:theme=\"@style/FullscreenTheme\" >\n"
                        + "-             <intent-filter android:label=\"@string/title_activity_fullscreen\" >\n"
                        + "+             <intent-filter\n"
                        + "+                 android:label=\"@string/title_activity_fullscreen\"\n"
                        + "+                 android:scheme=\"http\" >\n"
                        + "                  <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "Fix for AndroidManifest.xml line 16: Set host:\n"
                        + "@@ -18 +18\n"
                        + "  \n"
                        + "-                 <data android:pathPrefix=\"/gizmos\" />\n"
                        + "+                 <data\n"
                        + "+                     android:host=\"|\"\n"
                        + "+                     android:pathPrefix=\"/gizmos\" />\n"
                        + "  \n"
                        + "Fix for AndroidManifest.xml line 16: Set scheme=\"http\":\n"
                        + "@@ -18 +18\n"
                        + "  \n"
                        + "-                 <data android:pathPrefix=\"/gizmos\" />\n"
                        + "+                 <data\n"
                        + "+                     android:pathPrefix=\"/gizmos\"\n"
                        + "+                     android:scheme=\"http\" />\n"
                        + "  \n");
    }

    public void testMultiData() {
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".FullscreenActivity\"\n"
                        + "            android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "            android:label=\"@string/title_activity_fullscreen\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\" />\n"
                        + "                <data android:host=\"example.com\" />\n"
                        + "                <data android:pathPrefix=\"/gizmos\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expectClean();
    }

    public void testMultiIntent() {
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".FullscreenActivity\"\n"
                        + "            android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "            android:label=\"@string/title_activity_fullscreen\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example.com\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expectClean();
    }

    public void testMultiIntentWithError() {
        String expected = ""
                + "AndroidManifest.xml:21: Error: At least one host must be specified [AppLinkUrlError]\n"
                + "                <data android:scheme=\"http\"\n"
                + "                ^\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".FullscreenActivity\"\n"
                        + "            android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "            android:label=\"@string/title_activity_fullscreen\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected)
                .verifyFixes().window(1)
                .expectFixDiffs(""
                        + "Fix for AndroidManifest.xml line 20: Set host:\n"
                        + "@@ -24 +24\n"
                        + "                  <data\n"
                        + "+                     android:host=\"|\"\n"
                        + "                      android:pathPrefix=\"/gizmos\"\n");
    }

    public void testNotExported() {
        String expected = ""
                + "AndroidManifest.xml:7: Error: Activity supporting ACTION_VIEW is not exported [AppLinkUrlError]\n"
                + "        <activity android:exported=\"false\"\n"
                + "        ^\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity android:exported=\"false\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example1.com\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\"/>\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example2.com\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <activity android:exported=\"true\">\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example1.com\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testOkWithResource() {
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          package=\"com.example.helloworld\">\n"
                        + "\n"
                        + "    <application\n"
                        + "            android:allowBackup=\"true\"\n"
                        + "            android:icon=\"@mipmap/ic_launcher\"\n"
                        + "            android:label=\"@string/app_name\"\n"
                        + "            android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "                android:name=\".FullscreenActivity\"\n"
                        + "                android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "                android:label=\"@string/title_activity_fullscreen\"\n"
                        + "                android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                      android:host=\"example.com\"\n"
                        + "                      android:pathPrefix=\"@string/path_prefix\"\n"
                        + "                      android:port=\"@string/port\"/>\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                xml("res/values/appindexing_strings.xml", ""
                        + "<resources>\n"
                        + "    <string name=\"path_prefix\">/pathprefix</string>\n"
                        + "    <string name=\"port\">8080</string>\n"
                        + "</resources>\n"))
                .incremental("AndroidManifest.xml")
                .run()
                .expectClean();
    }

    public void testWrongWithResource() {
        String expected = ""
                + "AndroidManifest.xml:18: Error: android:pathPrefix attribute should start with /, but it is pathprefix [AppLinkUrlError]\n"
                + "                      android:pathPrefix=\"@string/path_prefix\"\n"
                + "                                          ~~~~~~~~~~~~~~~~~~~\n"
                + "AndroidManifest.xml:19: Error: not a valid port number [AppLinkUrlError]\n"
                + "                      android:port=\"@string/port\"/>\n"
                + "                                    ~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "          package=\"com.example.helloworld\">\n"
                        + "\n"
                        + "    <application\n"
                        + "            android:allowBackup=\"true\"\n"
                        + "            android:icon=\"@mipmap/ic_launcher\"\n"
                        + "            android:label=\"@string/app_name\"\n"
                        + "            android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "                android:name=\".FullscreenActivity\"\n"
                        + "                android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "                android:label=\"@string/title_activity_fullscreen\"\n"
                        + "                android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                      android:host=\"example.com\"\n"
                        + "                      android:pathPrefix=\"@string/path_prefix\"\n"
                        + "                      android:port=\"@string/port\"/>\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"),
                xml("res/values/appindexing_wrong_strings.xml", ""
                        + "\n"
                        + "<resources>\n"
                        + "    <string name=\"path_prefix\">pathprefix</string>\n"
                        + "    <string name=\"port\">gizmos</string>\n"
                        + "</resources>\n"))
                .incremental("AndroidManifest.xml")
                .run()
                .expect(expected);
    }

    public void testNoUrl() {
        String expected = ""
                + "AndroidManifest.xml:15: Error: Missing URL [AppLinkUrlError]\n"
                + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                + "            ^\n"
                + "AndroidManifest.xml:17: Error: Missing URL for the intent filter [AppLinkUrlError]\n"
                + "                <data />\n"
                + "                ~~~~~~~~\n"
                + "2 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".FullscreenActivity\"\n"
                        + "            android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "            android:label=\"@string/title_activity_fullscreen\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testMimeType() {
        String expected = ""
                + "AndroidManifest.xml:15: Error: Missing URL [AppLinkUrlError]\n"
                + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                + "            ^\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".FullscreenActivity\"\n"
                        + "            android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "            android:label=\"@string/title_activity_fullscreen\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:mimeType=\"mimetype\" /> "
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testDataMissing() {
        String expected = ""
                + "AndroidManifest.xml:15: Error: Missing data element [AppLinkUrlError]\n"
                + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                + "            ^\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".FullscreenActivity\"\n"
                        + "            android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "            android:label=\"@string/title_activity_fullscreen\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "                <category android:name=\"android.intent.category.BROWSABLE\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testNotBrowsable() {
        String expected = ""
                + "AndroidManifest.xml:25: Error: Activity supporting ACTION_VIEW is not set as BROWSABLE [AppLinkUrlError]\n"
                + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                + "            ^\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    package=\"com.example.helloworld\" >\n"
                        + "\n"
                        + "    <application\n"
                        + "        android:allowBackup=\"true\"\n"
                        + "        android:icon=\"@mipmap/ic_launcher\"\n"
                        + "        android:label=\"@string/app_name\"\n"
                        + "        android:theme=\"@style/AppTheme\" >\n"
                        + "        <activity\n"
                        + "            android:name=\".MainActivity\"\n"
                        + "            android:label=\"@string/app_name\" >\n"
                        + "            <intent-filter>\n"
                        + "                <action android:name=\"android.intent.action.MAIN\" />\n"
                        + "\n"
                        + "                <category android:name=\"android.intent.category.LAUNCHER\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "\n"
                        + "        <activity\n"
                        + "            android:name=\".FullscreenActivity\"\n"
                        + "            android:configChanges=\"orientation|keyboardHidden|screenSize\"\n"
                        + "            android:label=\"@string/title_activity_fullscreen\"\n"
                        + "            android:theme=\"@style/FullscreenTheme\" >\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "                <action android:name=\"android.intent.action.VIEW\" />\n"
                        + "                <data android:scheme=\"http\"\n"
                        + "                    android:host=\"example.com\"\n"
                        + "                    android:pathPrefix=\"/gizmos\" />\n"
                        + "                <category android:name=\"android.intent.category.DEFAULT\" />\n"
                        + "            </intent-filter>\n"
                        + "        </activity>\n"
                        + "        <meta-data android:name=\"com.google.android.gms.version\" android:value=\"@integer/google_play_services_version\" />\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expect(expected);
    }

    public void testDataBinding() {
        // When using data binding don't give incorrect validation messages such as
        // uppercase usage, missing slash prefix etc.

        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\""
                        + "    package=\"test.pkg\" >\n"
                        + "\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter android:autoVerify=\"true\">\n"
                        + "                <data android:scheme=\"@={Schemes.default}\"\n"
                        + "                    android:host=\"@{Hosts.lookup}\"\n"
                        + "                    android:pathPrefix=\"@{Prefixes.lookup}\" />\n"
                        + "            </intent-filter>\n"
                        + "            <tools:validation testUrl=\"http://example.com/gizmos/foo/bar\"/>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expectClean();
    }

    public void test37343746() {
        // Regression test for https://issuetracker.google.com/issues/37343746

        //noinspection all // Sample code
        lint().files(
                xml("AndroidManifest.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\""
                        + "    package=\"test.pkg\" >\n"
                        + "\n"
                        + "    <application>\n"
                        + "        <activity>\n"
                        + "            <intent-filter>\n"
                        + "                 <action android:name=\"android.intent.action.PROVIDER_CHANGED\"/>\n"
                        + "                 <data android:scheme=\"content\"/>\n"
                        + "                 <data android:host=\"${applicationId}.provider\"/>\n"
                        + "                 <data android:path=\"/beep/boop\"/>\n"
                        + "                 <data android:mimeType=\"*/*\"/>\n"
                        + "             </intent-filter>\n"
                        + "        </activity>\n"
                        + "    </application>\n"
                        + "\n"
                        + "</manifest>\n"))
                .run()
                .expectClean();
    }

    public void testStaticValidation()
            throws IOException, SAXException, ParserConfigurationException {
        // Usage outside of lint
        Document document = XmlUtils.parseDocument(""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                + "    package=\"test.pkg\" >\n"
                + "\n"
                + "    <application>\n"
                + "        <activity>\n"
                + "            <intent-filter android:autoVerify=\"true\">\n"
                + "                <data android:scheme=\"http\"\n"
                + "                    android:host=\"example.com\"\n"
                + "                    android:pathPrefix=\"/gizmos\" />\n"
                + "                <data android:path=\"/literal/path\" />\n"
                + "            </intent-filter>\n"
                + "            <tools:validation testUrl=\"http://example.com/gizmos/foo/bar\"/>\n"
                + "            <tools:validation testUrl=\"http://example.com/notmatch/foo/bar\"/>\n"
                + "            <tools:validation testUrl=\"http://notmatch.com/gizmos/foo/bar\"/>\n"
                + "            <tools:validation testUrl=\"https://example.com/gizmos/foo/bar\"/>\n"
                + "            <tools:validation testUrl=\"http://example.com/literal/path\"/>\n"
                + "        </activity>\n"
                + "    </application>\n"
                + "\n"
                + "</manifest>\n", true);
        Element root = document.getDocumentElement();
        Element application = XmlUtils.getFirstSubTag(root);
        Element activity = XmlUtils.getFirstSubTag(application);
        assertThat(activity).isNotNull();

        List<UriInfo> infos = AppLinksValidDetector.createUriInfos(activity, null);

        assertThat(AppLinksValidDetector.testElement(
                new URL("http://example.com/literal/path"), infos))
                .isNull(); // success

        assertThat(AppLinksValidDetector.testElement(
                new URL("http://example.com/gizmos/foo/bar"), infos))
                .isNull(); // success

        assertThat(AppLinksValidDetector.testElement(
                new URL("https://example.com/gizmos/foo/bar"), infos))
                .isEqualTo("Test URL did not match scheme http");

        assertThat(AppLinksValidDetector.testElement(
                new URL("http://example.com/notmatch/foo/bar"), infos))
                .isEqualTo("Test URL did not match path prefix /gizmos, path literal /literal/path");

        assertThat(AppLinksValidDetector.testElement(
                new URL("http://notmatch.com/gizmos/foo/bar"), infos))
                .isEqualTo("Test URL did not match host example.com");
    }
}
