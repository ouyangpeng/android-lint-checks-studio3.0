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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.VALUE_FALSE;
import static com.android.xml.AndroidManifest.ATTRIBUTE_REQUIRED;
import static com.android.xml.AndroidManifest.NODE_USES_FEATURE;
import static com.android.xml.AndroidManifest.NODE_USES_PERMISSION;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Detects various issues for Chrome OS devices.
 */
public class ChromeOsDetector extends Detector implements Detector.XmlScanner {
    private static final Implementation IMPLEMENTATION =
            new Implementation(ChromeOsDetector.class, Scope.MANIFEST_SCOPE);

    /** Using hardware unsupported by Chrome OS devices */
    public static final Issue UNSUPPORTED_CHROME_OS_HARDWARE = Issue.create(
            "UnsupportedChromeOsHardware", //$NON-NLS-1$
            "Unsupported Chrome OS Hardware Feature",
            "The `<uses-feature>` element should not require this unsupported Chrome OS hardware " +
            "feature. Any uses-feature not explicitly marked with `required=\"false\"` is " +
            "necessary on the device to be installed on. Ensure that any features that might " +
            "prevent it from being installed on a Chrome OS device are reviewed and marked as " +
            "not required in the manifest.",
            Category.CHROME_OS,
            6,
            Severity.ERROR,
            IMPLEMENTATION).setEnabledByDefault(false).
            addMoreInfo("https://developer.android.com/topic/arc/manifest.html#incompat-entries");

    /** Permission implies required hardware unsupported by Chrome OS */
    public static final Issue PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE = Issue.create(
            "PermissionImpliesUnsupportedChromeOsHardware", //$NON-NLS-1$
            "Permission Implies Unsupported Chrome OS Hardware",
            "The `<uses-permission>` element should not require a permission that implies an " +
            "unsupported Chrome OS hardware feature. Google Play assumes that certain " +
            "hardware related permissions indicate that the underlying hardware features are " +
            "required by default. To fix the issue, consider declaring the corresponding " +
            "uses-feature element with `required=\"false\"` attribute.",
            Category.CHROME_OS,
            3,
            Severity.ERROR,
            IMPLEMENTATION).setEnabledByDefault(false).
            addMoreInfo("https://developer.android.com/topic/arc/manifest.html#implied-features");

    private static final String HARDWARE_FEATURE_CAMERA = "android.hardware.camera"; //$NON-NLS-1$

    private static final String HARDWARE_FEATURE_CAMERA_AUTOFOCUS =
            "android.hardware.camera.autofocus"; //$NON-NLS-1$

    private static final String HARDWARE_FEATURE_TELEPHONY =
            "android.hardware.telephony"; //$NON-NLS-1$

    private static final String ANDROID_PERMISSION_CAMERA =
            "android.permission.CAMERA"; //$NON-NLS-1$

    // https://developer.android.com/topic/arc/manifest.html#incompat-entries
    private static final String[] UNSUPPORTED_HARDWARE_FEATURES = new String[] {
            HARDWARE_FEATURE_CAMERA,
            HARDWARE_FEATURE_CAMERA_AUTOFOCUS,
            "android.hardware.camera.capability.manual_post_processing",
            "android.hardware.camera.capability.manual_sensor",
            "android.hardware.camera.capability.raw",
            "android.hardware.camera.flash",
            "android.hardware.camera.level.full",
            "android.hardware.consumerir",
            "android.hardware.location.gps",
            "android.hardware.nfc",
            "android.hardware.nfc.hce",
            "android.hardware.sensor.barometer",
            HARDWARE_FEATURE_TELEPHONY,
            "android.hardware.telephony.cdma",
            "android.hardware.telephony.gsm",
            "android.hardware.touchscreen",
            "android.hardware.type.automotive",
            "android.hardware.type.television",
            "android.hardware.usb.accessory",
            "android.hardware.usb.host",
            // Partially-supported, only on some Chrome OS devices.
            "android.hardware.sensor.accelerometer",
            "android.hardware.sensor.compass",
            "android.hardware.sensor.gyroscope",
            "android.hardware.sensor.light",
            "android.hardware.sensor.proximity",
            "android.hardware.sensor.stepcounter",
            "android.hardware.sensor.stepdetector",
            // Software features not currently supported on Chrome OS devices.
            "android.software.app_widgets",
            "android.software.device_admin",
            "android.software.home_screen",
            "android.software.input_methods",
            "android.software.leanback",
            "android.software.live_wallpaper",
            "android.software.live_tv",
            "android.software.managed_users",
            "android.software.midi",
            "android.software.sip",
            "android.software.sip.voip"
    };

    /** Constructs a new {@link ChromeOsDetector} check */
    public ChromeOsDetector() {}

    /** Used for {@link #PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE} */
    private boolean usesFeatureCamera;

    /** Used for {@link #PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE} */
    private boolean usesFeatureCameraAutofocus;

    /** All permissions that imply unsupported Chrome OS hardware. */
    private List<String> unsupportedHardwareImpliedPermissions;

    /** All Unsupported Chrome OS uses features in use by the current manifest.*/
    private Set<String> allUnsupportedChromeOsUsesFeatures;

    /** Set containing unsupported Chrome OS uses-features elements without required="false" */
    private Set<String> unsupportedChromeOsUsesFeatures;

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(NODE_USES_FEATURE, NODE_USES_PERMISSION);
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        usesFeatureCamera = false;
        usesFeatureCameraAutofocus = false;
        unsupportedHardwareImpliedPermissions = Lists.newArrayListWithExpectedSize(2);
        unsupportedChromeOsUsesFeatures = Sets.newHashSetWithExpectedSize(2);
        allUnsupportedChromeOsUsesFeatures = Sets.newHashSetWithExpectedSize(2);
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        XmlContext xmlContext = (XmlContext) context;

        if (!context.getMainProject().isLibrary()) {
            // Report all unsupported Chrome OS hardware uses-feature.
            // These point to all unsupported Chrome OS uses features that have not be marked
            // required = false;
            if (!unsupportedChromeOsUsesFeatures.isEmpty()
                    && xmlContext.isEnabled(UNSUPPORTED_CHROME_OS_HARDWARE)) {
                List<Element> usesFeatureElements = AndroidTvDetector.findUsesFeatureElements(
                        unsupportedChromeOsUsesFeatures, xmlContext.document);
                for (Element element : usesFeatureElements) {
                    Attr attrRequired = element.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_REQUIRED);
                    Node location = attrRequired == null ? element : attrRequired;
                    LintFix fix = fix().set(ANDROID_URI, ATTRIBUTE_REQUIRED, VALUE_FALSE).build();
                    xmlContext.report(UNSUPPORTED_CHROME_OS_HARDWARE, location,
                            xmlContext.getLocation(location),
                            "Expecting `android:required=\"false\"` for this hardware "
                                    + "feature that may not be supported by all Chrome OS "
                                    + "devices.", fix);
                }
            }

            // Report permissions implying unsupported hardware
            if (!unsupportedHardwareImpliedPermissions.isEmpty()
                    && xmlContext.isEnabled(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)) {
                Predicate<String> p = (String input) -> {
                    // Special-case handling for camera permission - needs to check that
                    // both camera and camera autofocus features are present and set to
                    // android:required="false".
                    if (ANDROID_PERMISSION_CAMERA.equals(input)) {
                        return (!usesFeatureCamera || !usesFeatureCameraAutofocus);
                    }
                    // Filter out all permissions that already have their corresponding
                    // implied hardware declared in the AndroidManifest.xml.
                    String usesFeature = input != null
                            ? getImpliedUnsupportedHardware(input) : null;
                    return usesFeature != null
                            && !allUnsupportedChromeOsUsesFeatures.contains(usesFeature);
                };

                Collection<String> filteredPermissions =
                        unsupportedHardwareImpliedPermissions
                        .stream()
                        .filter(p)
                        .collect(Collectors.toCollection(HashSet::new));

                List<Element> permissionsWithoutUsesFeatures =
                        AndroidTvDetector.findPermissionElements(
                                filteredPermissions, xmlContext.document);

                for (Element permissionElement : permissionsWithoutUsesFeatures) {
                    String name = permissionElement.getAttributeNS(ANDROID_URI, ATTR_NAME);
                    String[] unsupportedHardwareNames = new String[2];
                    unsupportedHardwareNames[0] = getImpliedUnsupportedHardware(name);

                    // Special-case handling of camera permission - either or both implied features
                    // might be missing.
                    if (ANDROID_PERMISSION_CAMERA.equals(name)) {
                        if (usesFeatureCamera) {
                            unsupportedHardwareNames[0] = null;
                        }
                        if (!usesFeatureCameraAutofocus) {
                            unsupportedHardwareNames[1] = HARDWARE_FEATURE_CAMERA_AUTOFOCUS;
                        }
                    }

                    for (String unsupportedHardwareName : unsupportedHardwareNames) {
                        if (unsupportedHardwareName != null) {
                            String message = String.format(
                              "Permission exists without corresponding hardware `<uses-feature "
                              + "android:name=\"%1$s\" required=\"false\">` tag.", unsupportedHardwareName);
                            LintFix fix = fix().data(unsupportedHardwareName);
                            xmlContext.report(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE,
                                    permissionElement, xmlContext.getLocation(permissionElement),
                                    message, fix);
                        }
                    }
                }
            }
        }
    }

    @Nullable
    private static String getImpliedUnsupportedHardware(@NonNull String permission) {
        switch (permission) {
            case ANDROID_PERMISSION_CAMERA:
                return HARDWARE_FEATURE_CAMERA;
            case "android.permission.CALL_PHONE":
                return HARDWARE_FEATURE_TELEPHONY;
            case "android.permission.CALL_PRIVILEGED":
                return HARDWARE_FEATURE_TELEPHONY;
            case "android.permission.MODIFY_PHONE_STATE":
                return HARDWARE_FEATURE_TELEPHONY;
            case "android.permission.PROCESS_OUTGOING_CALLS":
                return HARDWARE_FEATURE_TELEPHONY;
            case "android.permission.READ_SMS":
                return HARDWARE_FEATURE_TELEPHONY;
            case "android.permission.RECEIVE_SMS":
                return HARDWARE_FEATURE_TELEPHONY;
            case "android.permission.RECEIVE_MMS":
                return HARDWARE_FEATURE_TELEPHONY;
            case "android.permission.RECEIVE_WAP_PUSH":
                return HARDWARE_FEATURE_TELEPHONY;
            case "android.permission.SEND_SMS":
                return HARDWARE_FEATURE_TELEPHONY;
            case "android.permission.WRITE_APN_SETTINGS":
                return HARDWARE_FEATURE_TELEPHONY;
            case "android.permission.WRITE_SMS":
                return HARDWARE_FEATURE_TELEPHONY;
            default:
                return null;
        }
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String elementName = element.getTagName();

        if (NODE_USES_FEATURE.equals(elementName)) {
            // Ensures that unsupported hardware features aren't required.
            Attr name = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
            if (name != null) {
                String featureName = name.getValue();
                if (isUnsupportedHardwareFeature(featureName)) {
                    allUnsupportedChromeOsUsesFeatures.add(featureName);
                    Attr required = element.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_REQUIRED);
                    if (required == null || Boolean.parseBoolean(required.getValue())) {
                        unsupportedChromeOsUsesFeatures.add(featureName);
                    }
                    // Special-case tracking of features implicitly needed by camera permission.
                    if (HARDWARE_FEATURE_CAMERA.equals(featureName)) {
                        usesFeatureCamera = true;
                    }
                    if (HARDWARE_FEATURE_CAMERA_AUTOFOCUS.equals(featureName)) {
                        usesFeatureCameraAutofocus = true;
                    }
                }
            }
        } else if (NODE_USES_PERMISSION.equals(elementName)) {
            // Store all <uses-permission> tags that imply unsupported hardware
            String permissionName = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
            if (getImpliedUnsupportedHardware(permissionName) != null) {
                unsupportedHardwareImpliedPermissions.add(permissionName);
            }
        }
    }

    private static boolean isUnsupportedHardwareFeature(@NonNull String featureName) {
        for (String prefix : UNSUPPORTED_HARDWARE_FEATURES) {
            if (featureName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
