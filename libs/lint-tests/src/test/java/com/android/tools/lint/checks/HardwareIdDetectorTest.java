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

public class HardwareIdDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new HardwareIdDetector();
    }

    public void testBluetoothAdapterGetAddressCall() throws Exception {
        assertEquals(""
                + "src/test/pkg/AppUtils.java:8: Warning: Using getAddress to get device identifiers is not recommended. [HardwareIds]\n"
                + "        return adapter.getAddress();\n"
                + "               ~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
                lintProject(
                        java("src/test/pkg/AppUtils.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.bluetooth.BluetoothAdapter;\n"
                                + "\n"
                                + "public class AppUtils {\n"
                                + "    public String getBAddress() {\n"
                                + "        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();\n"
                                + "        return adapter.getAddress();\n"
                                + "    }\n"
                                + "}\n")));
    }

    public void testGetAddressCallInCatchBlock() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject(
                        java("src/com/google/android/gms/common/GooglePlayServicesNotAvailableException.java", ""
                                + "package com.google.android.gms.common;\n"
                                + "public class GooglePlayServicesNotAvailableException extends Exception {\n"
                                + "}\n"),
                        java("src/com/google/android/gms/dummy/GmsDummyClient.java", ""
                                + "package com.google.android.gms.dummy;"
                                + "import com.google.android.gms.common.GooglePlayServicesNotAvailableException;"
                                + "public class GmsDummyClient {\n"
                                + "    public static String getId() throws GooglePlayServicesNotAvailableException {\n"
                                + "        return \"dummyId\";\n"
                                + "    }\n"
                                + "}\n"),
                        java("src/test/pkg/AppUtils.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.bluetooth.BluetoothAdapter;\n"
                                + "import android.content.Context;\n"
                                + "\n"
                                + "import com.google.android.gms.dummy.GmsDummyClient;\n"
                                + "import com.google.android.gms.common.GooglePlayServicesNotAvailableException;\n"
                                + "\n"
                                + "import java.io.IOException;\n"
                                + "\n"
                                + "public class AppUtils {\n"
                                + "\n"
                                + "    public String getAdvertisingId(Context context) {\n"
                                + "        try {\n"
                                + "            return GmsDummyClient.getId();\n"
                                + "        } catch (RuntimeException | GooglePlayServicesNotAvailableException e) {\n"
                                + "            // not available so get one of the ids.\n"
                                + "            return BluetoothAdapter.getDefaultAdapter().getAddress();\n"
                                + "        }\n"
                                + "    }\n"
                                + "}")));
    }

    public void testGetAndroidId() throws Exception {
        assertEquals(""
                + "src/test/pkg/AppUtils.java:9: Warning: Using getString to get device identifiers is not recommended. [HardwareIds]\n"
                + "        return Settings.Secure.getString(context.getContentResolver(), androidId);\n"
                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
                lintProject(
                        java("src/test/pkg/AppUtils.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.Context;\n"
                                + "import android.provider.Settings;\n"
                                + "\n"
                                + "public class AppUtils {\n"
                                + "    public String getAndroidId(Context context) {\n"
                                + "        String androidId = Settings.Secure.ANDROID_ID;\n"
                                + "        return Settings.Secure.getString(context.getContentResolver(), androidId);\n"
                                + "    }\n"
                                + "}\n")));
    }

    public void testWifiInfoGetMacAddress() throws Exception {
        assertEquals(""
                + "src/test/pkg/AppUtils.java:8: Warning: Using getMacAddress to get device identifiers is not recommended. [HardwareIds]\n"
                + "        return info.getMacAddress();\n"
                + "               ~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
                lintProject(
                        java("src/test/pkg/AppUtils.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.Context;\n"
                                + "import android.net.wifi.WifiInfo;\n"
                                + "\n"
                                + "public class AppUtils {\n"
                                + "    public String getMacAddress(WifiInfo info) {\n"
                                + "        return info.getMacAddress();\n"
                                + "    }\n"
                                + "}\n")));
    }

    public void testTelephoneManagerIdentifierCalls() throws Exception {
        assertEquals(""
                + "src/test/pkg/AppUtils.java:8: Warning: Using getDeviceId to get device identifiers is not recommended. [HardwareIds]\n"
                + "        return info.getDeviceId();\n"
                + "               ~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/AppUtils.java:11: Warning: Using getLine1Number to get device identifiers is not recommended. [HardwareIds]\n"
                + "        return info.getLine1Number();\n"
                + "               ~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/AppUtils.java:14: Warning: Using getSimSerialNumber to get device identifiers is not recommended. [HardwareIds]\n"
                + "        return info.getSimSerialNumber();\n"
                + "               ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/AppUtils.java:17: Warning: Using getSubscriberId to get device identifiers is not recommended. [HardwareIds]\n"
                + "        return info.getSubscriberId();\n"
                + "               ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 4 warnings\n",
                lintProject(
                        java("src/test/pkg/AppUtils.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.Context;\n"
                                + "import android.telephony.TelephonyManager;\n"
                                + "\n"
                                + "public class AppUtils {\n"
                                + "    public String getDeviceId(TelephonyManager info) {\n"
                                + "        return info.getDeviceId();\n"
                                + "    }\n"
                                + "    public String getLine1Number(TelephonyManager info) {\n"
                                + "        return info.getLine1Number();\n"
                                + "    }\n"
                                + "    public String getSerial(TelephonyManager info) {\n"
                                + "        return info.getSimSerialNumber();\n"
                                + "    }\n"
                                + "    public String getSubscriberId(TelephonyManager info) {\n"
                                + "        return info.getSubscriberId();\n"
                                + "    }\n"
                                + "}\n")));
    }

    public void testBuildSerialUsage() throws Exception {
        assertEquals("src/test/pkg/HardwareIdDetectorTestData.java:16: Warning: Using SERIAL to get device identifiers is not recommended. [HardwareIds]\n"
                        + "            serial = SERIAL;\n"
                        + "                     ~~~~~~\n"
                        + "src/test/pkg/HardwareIdDetectorTestData.java:21: Warning: Using ro.serialno to get device identifiers is not recommended. [HardwareIds]\n"
                        + "                serial = (String) get.invoke(null, \"ro.serialno\");\n"
                        + "                                                   ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/HardwareIdDetectorTestData.java:29: Warning: Using SERIAL to get device identifiers is not recommended. [HardwareIds]\n"
                        + "        return android.os.Build.SERIAL;\n"
                        + "                                ~~~~~~\n"
                        + "src/test/pkg/HardwareIdDetectorTestData.java:37: Warning: Using ro.serialno to get device identifiers is not recommended. [HardwareIds]\n"
                        + "            return (String) get.invoke(null, \"ro.serialno\");\n"
                        + "                                             ~~~~~~~~~~~~~\n"
                        + "0 errors, 4 warnings\n",
                lintProject(
                        java("src/test/pkg/HardwareIdDetectorTestData.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.Context;\n"
                                + "\n"
                                + "import java.lang.reflect.Method;\n"
                                + "\n"
                                + "import static android.os.Build.*;\n"
                                + "\n"
                                + "public class HardwareIdDetectorTestData {\n"
                                + "\n"
                                + "    // Fails because of the use of `ro.serialno` using reflection\n"
                                + "    // and Build.SERIAL static field access to hardware Id.\n"
                                + "    public static String getSerialNumber(Context context) {\n"
                                + "        String serial = null;\n"
                                + "        if (VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {\n"
                                + "            serial = SERIAL;\n"
                                + "        } else {\n"
                                + "            try {\n"
                                + "                Class<?> c = Class.forName(\"android.os.SystemProperties\");\n"
                                + "                Method get = c.getMethod(\"get\", String.class);\n"
                                + "                serial = (String) get.invoke(null, \"ro.serialno\");\n"
                                + "            } catch (Exception ig) {\n"
                                + "            }\n"
                                + "        }\n"
                                + "        return serial;\n"
                                + "    }\n"
                                + "\n"
                                + "    public static String getSerialNumber2() {\n"
                                + "        return android.os.Build.SERIAL;\n"
                                + "    }\n"
                                + "    public static String getSerialNumber3() {\n"
                                + "        try {\n"
                                + "            Class<?> c;\n"
                                + "            Method get;\n"
                                + "            c = Class.forName(\"android.os.SystemProperties\");\n"
                                + "            get = c.getMethod(\"get\", String.class);\n"
                                + "            return (String) get.invoke(null, \"ro.serialno\");\n"
                                + "        } catch (Exception ig) {\n"
                                + "            return null;"
                                + "        }\n"
                                + "    }\n"
                                + "\n"
                                + "}")));
    }

    public void testRoSerialUsage() throws Exception {
        assertEquals("src/test/pkg/AppUtils.java:17: Warning: Using ro.serialno to get device identifiers is not recommended. [HardwareIds]\n"
                        + "        return getSystemProperty(context, \"ro.serialno\");\n"
                        + "                                          ~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n",
                lintProject(
                        java("src/test/pkg/AppUtils.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.Context;\n"
                                + "import java.lang.reflect.Method;\n"
                                + "\n"
                                + "public class AppUtils {\n"
                                + "\n"
                                + "    public static String getSystemProperty(Context context, String key) throws Exception {\n"
                                + "\n"
                                + "        Class<?> c = context.getClassLoader()\n"
                                + "                .loadClass(\"android.os.SystemProperties\");\n"
                                + "        Method get = c.getMethod(\"get\", String.class);\n"
                                + "        return (String) get.invoke(null, key);\n"
                                + "    }\n"
                                + "\n"
                                + "    public static String getSerialProperty(Context context) throws Exception {\n"
                                + "        return getSystemProperty(context, \"ro.serialno\");\n"
                                + "    }\n"
                                + "}")
                ));
    }

    public void testMultipleRoSerialUsages() throws Exception {
        assertEquals("src/test/pkg/AppUtils.java:19: Warning: Using ro.serialno to get device identifiers is not recommended. [HardwareIds]\n"
                        + "        return getSysProperty(RO_SERIAL, \"default\");\n"
                        + "                              ~~~~~~~~~\n"
                        + "src/test/pkg/AppUtils.java:34: Warning: Using ro.serialno to get device identifiers is not recommended. [HardwareIds]\n"
                        + "        String def = getSystemProperty(context, null, \"ro.serialno\");\n"
                        + "                                                      ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/AppUtils.java:35: Warning: Using ro.serialno to get device identifiers is not recommended. [HardwareIds]\n"
                        + "        return getSystemProperty(context, \"ro.serialno\", null);\n"
                        + "                                          ~~~~~~~~~~~~~\n"
                        + "0 errors, 3 warnings\n",
                lintProject(
                        java("src/com/google/android/gms/common/GooglePlayServicesNotAvailableException.java", ""
                                + "package com.google.android.gms.common;\n"
                                + "public class GooglePlayServicesNotAvailableException extends Exception {\n"
                                + "}\n"),
                        java("src/com/google/android/gms/dummy/GmsDummyClient.java", ""
                                + "package com.google.android.gms.dummy;"
                                + "import com.google.android.gms.common.GooglePlayServicesNotAvailableException;"
                                + "public class GmsDummyClient {\n"
                                + "    public static String getId() throws GooglePlayServicesNotAvailableException {\n"
                                + "        return \"dummyId\";\n"
                                + "    }\n"
                                + "}\n"),
                        java("src/test/pkg/AppUtils.java", ""
                                + "package test.pkg;\n"
                                + "\n"
                                + "import android.content.Context;\n"
                                + "\n"
                                + "import com.google.android.gms.dummy.GmsDummyClient;\n"
                                + "import com.google.android.gms.common.GooglePlayServicesNotAvailableException;\n"
                                + "\n"
                                + "import java.lang.reflect.Method;\n"
                                + "\n"
                                + "public class AppUtils {\n"
                                + "\n"
                                + "    public static final String RO_SERIAL = \"ro.serialno\";\n"
                                + "\n"
                                + "    public static String getSysProperty(String key, String defaultValue) throws Exception {\n"
                                + "       Class<?> s = Class.forName(\"android.os.SystemProperties\");\n"
                                + "       Method getDefault = s.getMethod(\"get\", String.class, String.class);\n"
                                + "       return (String) getDefault.invoke(s, key, defaultValue);\n"
                                + "    }"
                                + "    public static String getSerial2() throws Exception {\n"
                                + "        return getSysProperty(RO_SERIAL, \"default\");\n"
                                + "    }\n"
                                + "\n"
                                + "    public static String getSystemProperty(Context context, String key1, String key2) throws Exception {\n"
                                + "\n"
                                + "        Class<?> c = context.getClassLoader()\n"
                                + "                .loadClass(\"android.os.SystemProperties\");\n"
                                + "        Class<?> s = Class.forName(\"android.os.SystemProperties\");\n"
                                + "        Method get = c.getMethod(\"get\", String.class);\n"
                                + "        Method getDefault = s.getMethod(\"get\", String.class, String.class);\n"
                                + "        String x = (String)getDefault.invoke(s, key2, \"def\");\n"
                                + "        return (String) get.invoke(null, key1);\n"
                                + "    }\n"
                                + "\n"
                                + "    public static String getSerialProperty(Context context) throws Exception {\n"
                                + "        String def = getSystemProperty(context, null, \"ro.serialno\");\n"
                                + "        return getSystemProperty(context, \"ro.serialno\", null);\n"
                                + "    }\n"
                                + "\n"
                                + "    // Should not result in a warning since it's called within the catch block\n"
                                + "    public static String doPlayServicesCall(Context context) throws Exception {\n"
                                + "        try {\n"
                                + "            return GmsDummyClient.getId();\n"
                                + "        } catch (RuntimeException | GooglePlayServicesNotAvailableException e) {\n"
                                + "            // not available so get one of the ids.\n"
                                + "            return getSystemProperty(context, \"ro.serialno\", \"ID\");\n"
                                + "        }\n"
                                + "    }\n"
                                + "}")
                ));
    }
}