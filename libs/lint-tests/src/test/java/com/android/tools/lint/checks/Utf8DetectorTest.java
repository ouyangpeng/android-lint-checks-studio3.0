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

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class Utf8DetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new Utf8Detector();
    }

    public void testIsoLatin() throws Exception {
        String expected = ""
                + "res/layout/encoding.xml:1: Error: iso-latin-1: Not using UTF-8 as the file encoding. This can lead to subtle bugs with non-ascii characters [EnforceUTF8]\n"
                + "<?xml version=\"1.0\" encoding=\"iso-latin-1\"?>\n"
                + "                              ~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/encoding.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"iso-latin-1\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected)
                .expectFixDiffs(""
                        + "Fix for res/layout/encoding.xml line 0: Replace with utf-8:\n"
                        + "@@ -1 +1\n"
                        + "- <?xml version=\"1.0\" encoding=\"iso-latin-1\"?>\n"
                        + "+ <?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    }

    public void testRaw() throws Exception {
        //noinspection all // Sample code
        lint().files(
                xml("res/raw/encoding.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"iso-latin-1\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expectClean();
    }

    public void testWithWindowsCarriageReturn() throws Exception {
        String expected = ""
                + "res/layout/encoding2.xml:1: Error: iso-latin-1: Not using UTF-8 as the file encoding. This can lead to subtle bugs with non-ascii characters [EnforceUTF8]\n"
                + "<?xml version=\"1.0\" encoding=\"iso-latin-1\"?>\n"
                + "                              ~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                // encoding2.xml = encoding.xml but with \n => \r
                xml("res/layout/encoding2.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"iso-latin-1\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expect(expected);
    }

    public void testNegative() throws Exception {
        // Make sure we don't get warnings for a correct file
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/layout1.xml", ""
                        + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:orientation=\"vertical\" >\n"
                        + "\n"
                        + "    <include\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        layout=\"@layout/layout2\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button1\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "    <Button\n"
                        + "        android:id=\"@+id/button2\"\n"
                        + "        android:layout_width=\"wrap_content\"\n"
                        + "        android:layout_height=\"wrap_content\"\n"
                        + "        android:text=\"Button\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expectClean();
    }

    public void testNoProlog() throws Exception {
        //noinspection all // Sample code
        lint().files(
                xml("res/layout/activity_item_two_pane.xml", ""
                        + "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                        + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                        + "    android:layout_width=\"match_parent\"\n"
                        + "    android:layout_height=\"match_parent\"\n"
                        + "    android:layout_marginLeft=\"16dp\"\n"
                        + "    android:layout_marginRight=\"16dp\"\n"
                        + "    android:divider=\"?android:attr/dividerHorizontal\"\n"
                        + "    android:orientation=\"horizontal\"\n"
                        + "    android:showDividers=\"middle\"\n"
                        + "    tools:context=\".ItemListActivity\" >\n"
                        + "\n"
                        + "    <!--\n"
                        + "    This layout is a two-pane layout for the Items\n"
                        + "    master/detail flow. See res/values-large/refs.xml and\n"
                        + "    res/values-sw600dp/refs.xml for an example of layout aliases\n"
                        + "    that replace the single-pane version of the layout with\n"
                        + "    this two-pane version.\n"
                        + "\n"
                        + "    For more on layout aliases, see:\n"
                        + "    http://developer.android.com/training/multiscreen/screensizes.html#TaskUseAliasFilters\n"
                        + "    -->\n"
                        + "\n"
                        + "    <fragment\n"
                        + "        android:id=\"@+id/item_list\"\n"
                        + "        android:name=\"com.example.master.ItemListFragment\"\n"
                        + "        android:layout_width=\"0dp\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:layout_weight=\"1\"\n"
                        + "        tools:layout=\"@android:layout/list_content\" />\n"
                        + "\n"
                        + "    <FrameLayout\n"
                        + "        android:id=\"@+id/item_detail_container\"\n"
                        + "        android:layout_width=\"0dp\"\n"
                        + "        android:layout_height=\"match_parent\"\n"
                        + "        android:layout_weight=\"3\" />\n"
                        + "\n"
                        + "</LinearLayout>\n"))
                .run()
                .expectClean();
    }

    public void testImplicitUtf16() throws Exception {
        // Implicit encoding: Not currently checked
        //noinspection all // Sample code
        lint().files(
                base64gzip("res/layout/layout.xml", ""
                        + "H4sIAAAAAAAAAD2NXQ6CMBCEv+eeovLuD+8ol/ACajCSUEhKNRzVC+gxwGkb"
                        + "yWS6O5nZ6bzMCxU1E44Oy4sGz0jLQM+RgpIdB82aE0bZDVvBcuah1KgtvxfR"
                        + "iH84nmoMcjuxV6/lplYnNNJB6diUW+9yhtQSBJ9+/vLmoxkTMXOV66Uq9utu"
                        + "ksrXUj/rOBp80AAAAA=="))
                .run()
                .expectClean();
    }

    public void testUtf16WithByteOrderMark() throws Exception {
        String expected = ""
                + "res/layout/layout.xml:1: Error: UTF-16: Not using UTF-8 as the file encoding. This can lead to subtle bugs with non-ascii characters [EnforceUTF8]\n"
                + "<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n"
                + "                              ~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                base64gzip("res/layout/layout.xml", ""
                        + "H4sIAAAAAAAAAD2Nyw3CMBBE39lVBO7hc+EUyI0KQgF8wkeKE8kxiFJpAMpI"
                        + "GNsCrezd2ZmdGcZhpKDkiaUh40GNo+dGR8uaKUtmLNQzMS1H7U9iWy6R3VGx"
                        + "JZdqJVSywchvok2ui4qrtL2m9O/1jN6vLHelerFN9Ky1CwlWFdK81MEpuZ7F"
                        + "dNHFq1zM//DirR4UQXMQ64QK5v/ZRJSuhb4PW4YY9AAAAA=="))
                .run()
                .expect(expected);
    }

    public void testUtf16WithoutByteOrderMark() throws Exception {
        String expected = ""
                + "res/layout/layout.xml:1: Error: UTF-16: Not using UTF-8 as the file encoding. This can lead to subtle bugs with non-ascii characters [EnforceUTF8]\n"
                + "<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n"
                + "                              ~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                base64gzip("res/layout/layout.xml", ""
                        + "H4sIAAAAAAAAAD2Nyw3CMBBE39lVBO7hc+EUyI0KQgF8wkeKHckxiFJpAMoA"
                        + "xrZAK3t3dmZn3h8qau5YOgputHgGLvQ4loyZM2GmXohx7LU/iHWcEruhYU0p"
                        + "1UKoZoWR30ibUhcNZ2kHTfnf6hm9X1muSg1iu+TZahcTrCqmBamjU3Y9iumT"
                        + "S1D5lP/iwVM9KqJmJ9YLVUz/s0koXwt9ATc9kG3yAAAA"))
                .run()
                .expect(expected);
    }

    public void testUtf32WithByteOrderMark() throws Exception {
        String expected = ""
                + "res/layout/layout.xml:1: Error: UTF_32: Not using UTF-8 as the file encoding. This can lead to subtle bugs with non-ascii characters [EnforceUTF8]\n"
                + "<?xml version=\"1.0\" encoding=\"UTF_32\"?>\n"
                + "                              ~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                base64gzip("res/layout/layout.xml", ""
                        + "H4sIAAAAAAAAAG3PSQ7CMAyF4ax7isCeecuw4wSwRgxlkFoqpQVxVC4AxwD+"
                        + "oBfJQiw+pbFj13bu9XbOjTHDDSUKeFyRI6DGCRXOmKCNAbro6+5VE99s9X6n"
                        + "2hg7mNolFphjhRGGysV5psg0Xwsd8ao7qm+tmP1e68x0/op7XrRro9rCzJnr"
                        + "XdqhlLRbo95pJjvrXjWVmaWRYPZ/4o6H7qlH6rNRbVAuxnp/4pnJ2X9/cx84"
                        + "lQA75AEAAA=="))
                .run()
                .expect(expected);
    }

    public void testUtf32WithoutByteOrderMark() throws Exception {
        String expected = ""
                + "res/layout/layout.xml:1: Error: UTF_32: Not using UTF-8 as the file encoding. This can lead to subtle bugs with non-ascii characters [EnforceUTF8]\n"
                + "<?xml version=\"1.0\" encoding=\"UTF_32\"?>\n"
                + "                              ~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                base64gzip("res/layout/layout.xml", ""
                        + "H4sIAAAAAAAAAG3PyQ3CQAyFYTeQHgJ39ivLjQrgjFjCImWRkoAolQagDv6B"
                        + "N5KFOHyajD12bDObmtkCdxTIkeKGDDUaXFChxAxdjNDHUPdUNeHNXu8Pqg2x"
                        + "k6tdY4UlNphgrFyYZ47EvvN10JNUdWf1bRTz31udic5fYc+rdm1Vm7s5M72L"
                        + "OxQSd2vVO87kZz2qpnKztFK7/V944Kl77BH77FRbKxdigz/xxOX8vz+5N+5K"
                        + "hIDgAQAA"))
                .run()
                .expect(expected);
    }

    public void testUtf32LeWithoutByteOrderMark() throws Exception {
        String expected = ""
                + "res/layout/layout.xml:1: Error: UTF_32LE: Not using UTF-8 as the file encoding. This can lead to subtle bugs with non-ascii characters [EnforceUTF8]\n"
                + "<?xml version=\"1.0\" encoding=\"UTF_32LE\"?>\n"
                + "                              ~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                base64gzip("res/layout/layout.xml", ""
                        + "H4sIAAAAAAAAAG3QOw7CMAzG8c45RWHnvRaYYGKEGfEoD6mlUhoQR+UCcA7+"
                        + "kb5IFmL4qY1ju3aLLMvmeKJGhRwPlPBocUWDG6boYoQ+hjrnqok5B+UfVRtj"
                        + "Z1O7wRpLbDHBGCsslBPnmsGhQAc9yVV/Uf9WMfu+09Pp+Svue9fOQbWVmbdU"
                        + "XtqllrRjUO80k531pJrGzBLEm//wwQtvnVOP1GevWq+7GBv8iTtzZ7+d7r4q"
                        + "a5A16AEAAA=="))
                .run()
                .expect(expected);
    }

    public void testMacRoman() throws Exception {
        String expected = ""
                + "res/layout/layout.xml:1: Error: MacRoman: Not using UTF-8 as the file encoding. This can lead to subtle bugs with non-ascii characters [EnforceUTF8]\n"
                + "<?xml version=\"1.0\" encoding=\"MacRoman\"?>\n"
                + "                              ~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                base64gzip("res/layout/layout.xml", ""
                        + "H4sIAAAAAAAAABWKwQ0CMQwE/67C+B8O/k6uAj6IBkIIYCmxpVwOUQS90AG1"
                        + "kVuN5jPL87sWfOW2iKmn4/5AmDXZTfTh6RTT2WpUmgPwzjm8PGXBQUTAbXUt"
                        + "XYpoxmS1Zu3g3LjezTD23jz9vh8KAHyNLfC0GXgaOcAfmGrdCnoAAAA="))
                .run()
                .expect(expected);
    }

    public void testWindows1252() throws Exception {
        String expected = ""
                + "res/layout/layout.xml:1: Error: windows-1252: Not using UTF-8 as the file encoding. This can lead to subtle bugs with non-ascii characters [EnforceUTF8]\n"
                + "<?xml version=\"1.0\" encoding=\"windows-1252\"?>\n"
                + "                              ~~~~~~~~~~~~\n"
                + "1 errors, 0 warnings\n";
        //noinspection all // Sample code
        lint().files(
                base64gzip("res/layout/layout.xml", ""
                        + "H4sIAAAAAAAAABWKwQ3CMAxF757C+B5KK3FL0iVYILQBLCW2lATKqCwAc5B+"
                        + "Pb3L+3Z+54SvWCqrOBqPJ8Ioi64sd0cby6pbNeN0nmj2YA/G4OXBFTsBAffl"
                        + "Z2qcWCIumnOUBsb0600VQ2vF0e/zJQ9gr6F4O+wGO/Ts4Q9wZ2rzfgAAAA=="))
                .run()
                .expect(expected);
    }
}
