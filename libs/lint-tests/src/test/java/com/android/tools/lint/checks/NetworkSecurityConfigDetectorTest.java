/*
 * Copyright (C) 2016 The Android Open Source Project
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

import com.android.tools.lint.detector.api.Detector;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class NetworkSecurityConfigDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new NetworkSecurityConfigDetector();
    }


    public void testInvalidElementAndMissingDomain() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/xml/network_config.xml:4: Error: Unexpected element <include> [NetworkSecurityConfig]\n"
                + "     <include domain=\"file\"/>\n"
                + "      ~~~~~~~\n"
                + "res/xml/network_config.xml:7: Error: Nested <domain-config> elements are not allowed in base-config [NetworkSecurityConfig]\n"
                + "         <domain-config>\n"
                + "          ^\n"
                + "res/xml/network_config.xml:12: Error: No <domain> elements in <domain-config> [NetworkSecurityConfig]\n"
                + "     <domain-config>\n"
                + "      ^\n"
                + "3 errors, 0 warnings\n",
                lintProject(xml("res/xml/network_config.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<network-security-config>\n"
                        + "     <!-- Invalid element as child. -->\n"
                        + "     <include domain=\"file\"/>\n"
                        + "     <!-- Invalid base-config with nested domain-config element -->\n"
                        + "     <base-config>\n"
                        + "         <domain-config>\n"
                        + "             <domain>android.com</domain>\n"
                        + "         </domain-config>\n"
                        + "     </base-config>\n"
                        + "     <!-- Invalid domain-config without domain child element -->\n"
                        + "     <domain-config>\n"
                        + "     </domain-config>\n"
                        + "</network-security-config>")));
    }

    public void testTrustAnchors() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/xml/network_config.xml:7: Error: Unknown certificates src attribute. Expecting system, user or an @resource value [NetworkSecurityConfig]\n"
                + "            <certificates src=\"raw/extras\"/>\n"
                + "                               ~~~~~~~~~~\n"
                + "res/xml/network_config.xml:9: Error: Missing src attribute [NetworkSecurityConfig]\n"
                + "            <certificates/>\n"
                + "             ~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n",
                lintProject(xml("res/xml/network_config.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<network-security-config>\n"
                        + "    <domain-config>\n"
                        + "        <domain includeSubdomains=\"false\">example.com</domain>\n"
                        + "        <trust-anchors>\n"
                        + "            <!-- Invalid attribute only system, user or @resource allowed -->\n"
                        + "            <certificates src=\"raw/extras\"/>\n"
                        + "            <!-- Missing src attribute src attr -->\n"
                        + "            <certificates/>\n"
                        + "            <!-- valid src attr -->\n"
                        + "            <certificates src=\"user\"/>\n"
                        + "        </trust-anchors>\n"
                        + "    </domain-config>\n"
                        + "</network-security-config>")));
    }

    // Test expiration, invalid digest algorithm and invalid digest length for sha-256
    public void testPinSetElement() throws Exception {
        String fiveDaysFromNow = LocalDate.now()
                .plusDays(5)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        String expected = String.format(
                "res/xml/network_config.xml:6: Warning: pin-set is expiring soon [PinSetExpiry]\n"
                        + "        <pin-set expiration=\"%1$s\">\n"
                        + "                             ~~~~~~~~~~\n"
                        + "res/xml/network_config.xml:8: Error: Invalid digest algorithm. Supported digests: SHA-256 [NetworkSecurityConfig]\n"
                        + "            <pin digest=\"SHA-1\">7HIpactkIAq2Y49orFOOQKurWxmmSFZhBCoQYcRhJ3Y=</pin>\n"
                        + "                         ~~~~~\n"
                        + "res/xml/network_config.xml:10: Error: Decoded digest length 30 does not match expected length for SHA-256 of 32 [NetworkSecurityConfig]\n"
                        + "            <pin digest=\"SHA-256\">aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d</pin>\n"
                        + "                                  ^\n"
                        + "2 errors, 1 warnings\n", fiveDaysFromNow);
        //noinspection all // Sample code
        lint().files(
                xml("res/xml/network_config.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<network-security-config>\n"
                        + "    <domain-config>\n"
                        + "        <domain includeSubdomains=\"true\">example.com</domain>\n"
                        + "        <!-- pin-set is expiring soon -->\n"
                        + "        <pin-set expiration=\"" + fiveDaysFromNow + "\">\n"
                        + "            <!-- Invalid digest algorithm-->\n"
                        + "            <pin digest=\"SHA-1\">7HIpactkIAq2Y49orFOOQKurWxmmSFZhBCoQYcRhJ3Y=</pin>\n"
                        + "            <!-- Invalid SHA-256 digest length -->\n"
                        + "            <pin digest=\"SHA-256\">aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d</pin>\n"
                        + "        </pin-set>\n"
                        + "    </domain-config>\n"
                        + "</network-security-config>"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/xml/network_config.xml line 7: Set digest to \"SHA-256\":\n"
                        + "@@ -8 +8\n"
                        + "-             <pin digest=\"SHA-1\">7HIpactkIAq2Y49orFOOQKurWxmmSFZhBCoQYcRhJ3Y=</pin>\n"
                        + "+             <pin digest=\"SHA-256\">7HIpactkIAq2Y49orFOOQKurWxmmSFZhBCoQYcRhJ3Y=</pin>\n");
    }

    public void testMissingBackupPin() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/xml/network_config.xml:5: Warning: A backup <pin> declaration is highly recommended [MissingBackupPin]\n"
                + "        <pin-set>\n"
                + "         ^\n"
                + "0 errors, 1 warnings\n",
                lintProject(xml("res/xml/network_config.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<network-security-config>\n"
                        + "    <domain-config>\n"
                        + "        <domain includeSubdomains=\"true\">www.example.com</domain>\n"
                        + "        <pin-set>\n"
                        + "            <!-- no backup pin declared -->"
                        + "            <pin digest=\"SHA-256\">7HIpactkIAq2Y49orFOOQKurWxmmSFZhBCoQYcRhJ3Y=</pin>\n"
                        + "        </pin-set>\n"
                        + "    </domain-config>\n"
                        + "</network-security-config>")));
    }

    public void testInvalidMultiplePinSetElements() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/xml/network_config.xml:10: Error: Multiple <pin-set> elements are not allowed [NetworkSecurityConfig]\n"
                + "        <pin-set>\n"
                + "         ^\n"
                + "    res/xml/network_config.xml:5: Already declared here\n"
                + "1 errors, 0 warnings\n",
                lintProject(xml("res/xml/network_config.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<network-security-config>\n"
                        + "    <domain-config>\n"
                        + "        <domain includeSubdomains=\"true\">example.com</domain>\n"
                        + "        <pin-set>\n"
                        + "            <pin digest=\"SHA-256\">7HIpactkIAq2Y49orFOOQKurWxmmSFZhBCoQYcRhJ3Y=</pin>\n"
                        + "            <pin digest=\"SHA-256\">fwza0LRMXouZHRC8Ei+4PyuldPDcf3UKgO/04cDM1oE=</pin>\n"
                        + "        </pin-set>\n"
                        + "        <!-- Multiple pin-set elements are not allowed -->\n"
                        + "        <pin-set>\n"
                        + "            <pin digest=\"SHA-256\">fwza0LRMXouZHRC8Ei+4PyuldPDcf3UKgO/04cDM1oE=</pin>\n"
                        + "        </pin-set>\n"
                        + "    </domain-config>\n"
                        + "</network-security-config>")));
    }

    public void testNestedDomainConfigsWithDuplicateDomains() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/xml/network_config.xml:6: Error: Duplicate domain names are not allowed [NetworkSecurityConfig]\n"
                + "        <domain includeSubdomains=\"true\">www.Example.com</domain>\n"
                + "                                         ^\n"
                + "    res/xml/network_config.xml:4: Already declared here\n"
                + "res/xml/network_config.xml:13: Error: Duplicate domain names are not allowed [NetworkSecurityConfig]\n"
                + "            <domain includeSubdomains=\"true\">www.example.com</domain>\n"
                + "                                             ^\n"
                + "    res/xml/network_config.xml:4: Already declared here\n"
                + "2 errors, 0 warnings\n",
                lintProject(xml("res/xml/network_config.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<network-security-config>\n"
                        + "    <domain-config>\n"
                        + "        <domain includeSubdomains=\"true\">www.example.com</domain>\n"
                        + "        <!-- Duplicate domain registration case insensitive -->\n"
                        + "        <domain includeSubdomains=\"true\">www.Example.com</domain>\n"
                        + "        <pin-set>\n"
                        + "            <pin digest=\"SHA-256\">7HIpactkIAq2Y49orFOOQKurWxmmSFZhBCoQYcRhJ3Y=</pin>\n"
                        + "            <pin digest=\"SHA-256\">fwza0LRMXouZHRC8Ei+4PyuldPDcf3UKgO/04cDM1oE=</pin>\n"
                        + "        </pin-set>\n"
                        + "        <domain-config>\n"
                        + "            <!-- Nested domain-config with duplicate domain name -->\n"
                        + "            <domain includeSubdomains=\"true\">www.example.com</domain>\n"
                        + "        </domain-config>\n"
                        + "    </domain-config>\n"
                        + "</network-security-config>")));

    }

    public void testTrustAnchorsWithMissingCAResource() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/xml/network_config.xml:6: Error: Missing src resource. [NetworkSecurityConfig]\n"
                + "            <certificates src=\"@raw/my_ca\"/>\n"
                + "                               ~~~~~~~~~~\n"
                + "res/xml/network_config.xml:11: Error: Missing src resource. [NetworkSecurityConfig]\n"
                + "            <certificates src=\"@raw/debug_cas\"/>\n"
                + "                               ~~~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n",
                lintProjectIncrementally(
                        "res/xml/network_config.xml",
                         xml("res/xml/network_config.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<network-security-config>\n"
                                + "    <domain-config>\n"
                                + "        <domain includeSubdomains=\"true\">example.com</domain>\n"
                                + "        <trust-anchors>\n"
                                + "            <certificates src=\"@raw/my_ca\"/>\n"
                                + "        </trust-anchors>\n"
                                + "    </domain-config>\n"
                                + "    <debug-overrides>\n"
                                + "        <trust-anchors>\n"
                                + "            <certificates src=\"@raw/debug_cas\"/>\n"
                                + "        </trust-anchors>\n"
                                + "    </debug-overrides>"
                                + "</network-security-config>")));
    }

    public void testTrustAnchorsWithValidCAResource() throws Exception {
        //noinspection all // Sample code
        assertEquals(
                "No warnings.",
                lintProjectIncrementally(
                        "res/xml/network_config.xml",
                        // A valid cert/PEM is not necessary for the test since the lint detector
                        // only checks for the presence of the resource.
                        source("res/raw/debug_cas.xml", "" +
                                "-----BEGIN CERTIFICATE-----\n"
                                + "7vQMfXdGsRrXNGRGnX+vWDZ3/zWI0joDtCkNnqEpVn..HoX\n"
                                + "-----END CERTIFICATE-----"),
                        xml("res/xml/network_config.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<network-security-config>\n"
                                + "    <debug-overrides cleartextTrafficPermitted=\"true\">\n"
                                + "        <trust-anchors>\n"
                                + "            <certificates src=\"@raw/debug_cas\"/>\n"
                                + "        </trust-anchors>\n"
                                + "    </debug-overrides>"
                                + "</network-security-config>")));
    }

    public void testTyposInBaseTags() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "res/xml/network_config.xml:3: Error: Unexpected element <include> [NetworkSecurityConfig]\n"
                + "     <include domain=\"file\"/>\n"
                + "      ~~~~~~~\n"
                + "res/xml/network_config.xml:4: Error: Misspelled tag <base-cnofig>: Did you mean base-config ? [NetworkSecurityConfig]\n"
                + "     <base-cnofig>\n"
                + "      ^\n"
                + "res/xml/network_config.xml:6: Error: Misspelled attribute clearTxtTrafficPermitted: Did you mean cleartextTrafficPermitted ? [NetworkSecurityConfig]\n"
                + "     <domain-config invalidattr=\"true\" clearTxtTrafficPermitted=\"true\">\n"
                + "                                       ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "res/xml/network_config.xml:7: Error: Misspelled attribute includeSubdomain: Did you mean includeSubdomains ? [NetworkSecurityConfig]\n"
                + "        <domain includeSubdomain='true'>android.com</domain>\n"
                + "                ~~~~~~~~~~~~~~~~\n"
                + "res/xml/network_config.xml:8: Error: Misspelled tag <trustAnchor>: Did you mean trust-anchors ? [NetworkSecurityConfig]\n"
                + "        <trustAnchor>\n"
                + "         ^\n"
                + "res/xml/network_config.xml:15: Error: Misspelled tag <ceritficates>: Did you mean certificates ? [NetworkSecurityConfig]\n"
                + "            <ceritficates src=\"@raw/debug_cas\"/>\n"
                + "             ~~~~~~~~~~~~\n"
                + "res/xml/network_config.xml:21: Error: Misspelled attribute source: Did you mean src ? [NetworkSecurityConfig]\n"
                + "            <certificates source=\"@raw/debug_cas\"/>\n"
                + "                          ~~~~~~\n"
                + "res/xml/network_config.xml:24: Error: Misspelled attribute dgest: Did you mean digest ? [NetworkSecurityConfig]\n"
                + "            <pin dgest=\"SHA-256\">7HIpactkIAq2Y49orFOOQKurWxmmSFZhBCoQYcRhJ3Y=</pin>\n"
                + "                 ~~~~~\n"
                + "res/xml/network_config.xml:25: Error: Misspelled tag <pln>: Did you mean pin ? [NetworkSecurityConfig]\n"
                + "            <pln digest=\"SHA-256\">fwza0LRMXouZHRC8Ei+4PyuldPDcf3UKgO/04cDM1oE=</pln>\n"
                + "             ~~~\n"
                + "res/xml/network_config.xml:29: Error: Unexpected element <test-overrides> [NetworkSecurityConfig]\n"
                + "     <test-overrides>\n"
                + "      ^\n"
                + "res/xml/network_config.xml:32: Error: Misspelled tag <debug-ovrrides>: Did you mean debug-overrides ? [NetworkSecurityConfig]\n"
                + "     <debug-ovrrides></debug-ovrrides>\n"
                + "      ~~~~~~~~~~~~~~\n"
                + "11 errors, 0 warnings\n",
                lintProject(xml("res/xml/network_config.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<network-security-config>\n"
                        + "     <include domain=\"file\"/>\n"
                        + "     <base-cnofig>\n"
                        + "     </base-cnofig>\n"
                        + "     <domain-config invalidattr=\"true\" clearTxtTrafficPermitted=\"true\">\n"
                        + "        <domain includeSubdomain='true'>android.com</domain>\n"
                        + "        <trustAnchor>\n"
                        + "            <certificates src=\"@raw/debug_cas\"/>\n"
                        + "        </trustAnchor>\n"
                        + "     </domain-config>\n"
                        + "     <domain-config>\n"
                        + "        <domain includeSubdomains='true'>www.example.com</domain>\n"
                        + "        <trust-anchors>\n"
                        + "            <ceritficates src=\"@raw/debug_cas\"/>\n"
                        + "        </trust-anchors>\n"
                        + "     </domain-config>\n"
                        + "     <domain-config>\n"
                        + "        <domain includeSubdomains='true'>foo.example.com</domain>\n"
                        + "        <trust-anchors>\n"
                        + "            <certificates source=\"@raw/debug_cas\"/>\n"
                        + "        </trust-anchors>\n"
                        + "        <pin-set>\n"
                        + "            <pin dgest=\"SHA-256\">7HIpactkIAq2Y49orFOOQKurWxmmSFZhBCoQYcRhJ3Y=</pin>\n"
                        + "            <pln digest=\"SHA-256\">fwza0LRMXouZHRC8Ei+4PyuldPDcf3UKgO/04cDM1oE=</pln>\n"
                        + "        </pin-set>\n"
                        + "     </domain-config>\n"
                        + "     <!-- Unexpected element -->\n"
                        + "     <test-overrides>\n"
                        + "     </test-overrides>\n"
                        + "     <!-- spelling error -->\n"
                        + "     <debug-ovrrides></debug-ovrrides>\n"
                        + "</network-security-config>")));
    }

    public void testConfigDuplicatesMessage() throws Exception {
        //noinspection all // Sample code
        // Note that the _debug.xml resource can contain only <debug-overrides> elements
        assertEquals(""
                + "res/xml/network_config.xml:5: Error: Expecting at most 1 <base-config> [NetworkSecurityConfig]\n"
                + "    <base-config>\n"
                + "     ^\n"
                + "    res/xml/network_config.xml:3: Already declared here\n"
                + "res/xml/network_config.xml:12: Error: Expecting at most 1 <debug-overrides> [NetworkSecurityConfig]\n"
                + "    <debug-overrides>\n"
                + "     ^\n"
                + "    res/xml/network_config.xml:7: Already declared here\n"
                + "res/xml/network_config_debug.xml:3: Error: Expecting at most 1 <debug-overrides> [NetworkSecurityConfig]\n"
                + "    <debug-overrides>\n"
                + "     ^\n"
                + "    res/xml/network_config.xml:7: Already declared here\n"
                + "3 errors, 0 warnings\n",
                lintProject(xml("res/xml/network_config_debug.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<network-security-config>\n"
                                + "    <debug-overrides>\n"
                                + "        <trust-anchors>\n"
                                + "            <certificates src=\"@raw/debug_cas\"/>\n"
                                + "        </trust-anchors>\n"
                                + "    </debug-overrides>"
                                + "</network-security-config>"),
                        source("res/raw/debug_cas.xml", "" +
                                "-----BEGIN CERTIFICATE-----\n"
                                + "7vQMfXdGsRrXNGRGnX+vWDZ3/zWI0joDtCkNnqEpVn..HoX\n"
                                + "-----END CERTIFICATE-----"),
                        xml("res/xml/network_config.xml", ""
                                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                                + "<network-security-config>\n"
                                + "    <base-config>\n"
                                + "    </base-config>\n"
                                + "    <base-config>\n"
                                + "    </base-config>\n"
                                + "    <debug-overrides>\n"
                                + "        <trust-anchors>\n"
                                + "            <certificates src=\"@raw/debug_cas\"/>\n"
                                + "        </trust-anchors>\n"
                                + "    </debug-overrides>\n"
                                + "    <debug-overrides>\n"
                                + "    </debug-overrides>"
                                + "</network-security-config>")));
    }
}
