/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.SdkConstants.CLASS_ACTIVITY;
import static com.android.SdkConstants.CLASS_APPLICATION;
import static com.android.SdkConstants.CLASS_CONTEXT;
import static com.android.SdkConstants.CLASS_VIEW;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.UastLintUtils;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import java.util.Collections;
import java.util.List;
import org.jetbrains.uast.UBinaryExpressionWithType;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.util.UastExpressionUtils;

/**
 * Detector looking for casts on the result of context.getSystemService which are suspect.
 * <p>
 * TODO: As of O we can start looking for the @SystemService annotation on the target interface
 * class, and the value attribute will map back to the expected constant. This should let us
 * get rid of the hardcoded lookup table below.
 */
public class ServiceCastDetector extends Detector implements UastScanner {
    public static final Implementation IMPLEMENTATION = new Implementation(
        ServiceCastDetector.class,
        Scope.JAVA_FILE_SCOPE);

    /** Invalid cast to a type from the service constant */
    public static final Issue ISSUE = Issue.create(
            "ServiceCast",
            "Wrong system service casts",

            "When you call `Context#getSystemService()`, the result is typically cast to " +
            "a specific interface. This lint check ensures that the cast is compatible with " +
            "the expected type of the return value.",

            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            IMPLEMENTATION);

    /** Using wifi manager from the wrong context */
    public static final Issue WIFI_MANAGER = Issue.create(
            "WifiManagerLeak",
            "WifiManager Leak",

            "On versions prior to Android N (24), initializing the `WifiManager` via " +
            "`Context#getSystemService` can cause a memory leak if the context is not " +
            "the application context. Change `context.getSystemService(...)` to " +
            "`context.getApplicationContext().getSystemService(...)`.",

            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            IMPLEMENTATION);

    /** Using wifi manager from the wrong context: unknown Context origin */
    public static final Issue WIFI_MANAGER_UNCERTAIN = Issue.create(
            "WifiManagerPotentialLeak",
            "WifiManager Potential Leak",

            "On versions prior to Android N (24), initializing the `WifiManager` via " +
            "`Context#getSystemService` can cause a memory leak if the context is not " +
            "the application context.\n" +
            "\n" +
            "In many cases, it's not obvious from the code where the `Context` is " +
            "coming from (e.g. it might be a parameter to a method, or a field initialized " +
            "from various method calls.)  It's possible that the context being passed in " +
            "is the application context, but to be on the safe side, you should consider " +
            "changing `context.getSystemService(...)` to " +
            "`context.getApplicationContext().getSystemService(...)`.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION);

    private static final String GET_APPLICATION_CONTEXT = "getApplicationContext";
    private static final String WIFI_SERVICE = "WIFI_SERVICE";

    /** Constructs a new {@link ServiceCastDetector} check */
    public ServiceCastDetector() {
    }

    // ---- Implements UastScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("getSystemService");
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @NonNull UCallExpression call,
            @NonNull PsiMethod method) {
        List<UExpression> args = call.getValueArguments();
        if (args.size() == 1 && args.get(0) instanceof UReferenceExpression) {
            PsiElement resolvedServiceConst = ((UReferenceExpression) args.get(0)).resolve();
            if (!(resolvedServiceConst instanceof PsiField)) {
                return;
            }
            String name = ((PsiField) resolvedServiceConst).getName();

            // Check WIFI_SERVICE context origin
            if (WIFI_SERVICE.equals(name)) {
                checkWifiService(context, call);
            }

            UElement parent = LintUtils.skipParentheses(
                    UastUtils.getQualifiedParentOrThis(call).getUastParent());
            if (UastExpressionUtils.isTypeCast(parent)) {
                UBinaryExpressionWithType cast = (UBinaryExpressionWithType) parent;

                // Check cast
                String expectedClass = getExpectedType(name);
                if (expectedClass != null && cast != null) {
                    String castType = cast.getType().getCanonicalText();
                    if (castType.indexOf('.') == -1) {
                        expectedClass = stripPackage(expectedClass);
                    }
                    if (!castType.equals(expectedClass)) {
                        // It's okay to mix and match
                        // android.content.ClipboardManager and android.text.ClipboardManager
                        if (isClipboard(castType) && isClipboard(expectedClass)) {
                            return;
                        }

                        String message = String.format(
                                "Suspicious cast to `%1$s` for a `%2$s`: expected `%3$s`",
                                stripPackage(castType), name, stripPackage(expectedClass));
                        context.report(ISSUE, call, context.getLocation(cast), message);
                    }
                }
            }
        }
    }

    /**
     * Checks that the given call to {@code Context#getSystemService(WIFI_SERVICE)} is
     * using the application context
     */
    private static void checkWifiService(@NonNull JavaContext context,
            @NonNull UCallExpression call) {
        JavaEvaluator evaluator = context.getEvaluator();
        UExpression qualifier = call.getReceiver();
        PsiMethod resolvedMethod = call.resolve();
        if (resolvedMethod != null &&
                (evaluator.isMemberInSubClassOf(resolvedMethod, CLASS_ACTIVITY, false) ||
                        (evaluator.isMemberInSubClassOf(resolvedMethod, CLASS_VIEW, false)))) {
            reportWifiServiceLeak(WIFI_MANAGER, context, call);
            return;
        }
        if (qualifier == null) {
            // Implicit: check surrounding class
            UMethod currentMethod = UastUtils.getParentOfType(call, UMethod.class, true);
            if (currentMethod != null
                    && !evaluator.isMemberInSubClassOf(currentMethod, CLASS_APPLICATION, true)) {
                reportWifiServiceLeak(WIFI_MANAGER, context, call);
            }
        } else {
            checkContextReference(context, qualifier, call);
        }
    }

    /**
     * Given a reference to a context, check to see if the context is an application
     * context (in which case, return quietly), or known to not be an application context
     * (in which case, report an error), or is of an unknown context type (in which case,
     * report a warning).
     *
     * @param context the lint analysis context
     * @param element the reference to be checked
     * @param call    the original getSystemService call to report an error against
     */
    private static boolean checkContextReference(
            @NonNull JavaContext context,
            @Nullable UElement element,
            @NonNull UCallExpression call) {
        if (element == null) {
            return false;
        }
        if (element instanceof UCallExpression) {
            PsiMethod resolvedMethod = ((UCallExpression) element).resolve();
            if (resolvedMethod != null && !GET_APPLICATION_CONTEXT
                    .equals(resolvedMethod.getName())) {
                reportWifiServiceLeak(WIFI_MANAGER, context, call);
                return true;
            }
        } else if (element instanceof UQualifiedReferenceExpression) {
            UQualifiedReferenceExpression refExp = (UQualifiedReferenceExpression) element;
            PsiElement resolved = refExp.resolve();
            if (resolved instanceof PsiMethod && !GET_APPLICATION_CONTEXT
                    .equals(refExp.getResolvedName())) {
                reportWifiServiceLeak(WIFI_MANAGER, context, call);
                return true;
            }
        } else if (element instanceof UReferenceExpression) {
            // Check variable references backwards
            PsiElement resolved = ((UReferenceExpression) element).resolve();
            if (resolved instanceof PsiField) {
                PsiType type = ((PsiField) resolved).getType();
                return checkWifiContextType(context, call, type, true);
            } else if (resolved instanceof PsiParameter) {
                // Parameter: is the parameter type something other than just "Context"
                // or some subclass of Application?
                PsiType type = ((PsiParameter) resolved).getType();
                return checkWifiContextType(context, call, type, true);
            } else if (resolved instanceof PsiLocalVariable) {
                PsiLocalVariable variable = (PsiLocalVariable) resolved;
                PsiType type = variable.getType();
                if (checkWifiContextType(context, call, type, false)) {
                    return true;
                }

                // Walk backwards through assignments to find the most recent initialization
                // of this variable
                UExpression lastAssignment = UastLintUtils.findLastAssignment(variable, call);
                if (lastAssignment != null) {
                    return checkContextReference(context, lastAssignment, call);
                }
            }
        }

        return false;
    }

    /**
     * Given a context type (of a parameter or field), check to see if that type implies
     * that the context is not the application context (for example because it's an Activity
     * rather than a plain context).
     * <p>
     * Returns true if it finds and reports a problem.
     */
    private static boolean checkWifiContextType(@NonNull JavaContext context,
            @NonNull UCallExpression call, @NonNull PsiType type,
            boolean flagPlainContext) {
        JavaEvaluator evaluator = context.getEvaluator();
        if (type instanceof PsiClassType) {
            PsiClass psiClass = ((PsiClassType) type).resolve();
            if (evaluator.extendsClass(psiClass, CLASS_APPLICATION, false)) {
                return false;
            }
        }
        if (evaluator.typeMatches(type, CLASS_CONTEXT)) {
            if (flagPlainContext) {
                reportWifiServiceLeak(WIFI_MANAGER_UNCERTAIN, context, call);
                return true;
            }
            return false;
        }

        reportWifiServiceLeak(WIFI_MANAGER, context, call);
        return true;
    }

    private static void reportWifiServiceLeak(@NonNull Issue issue, @NonNull JavaContext context,
            @NonNull UCallExpression call) {
        if (context.getMainProject().getMinSdk() >= 24) {
            // Bug is fixed in Nougat
            return;
        }

        String message = "The WIFI_SERVICE must be looked up on the "
                + "Application context or memory will leak on devices < Android N. ";

        LintFix fix;
        if (call.getReceiver() != null) {
            String qualifier = call.getReceiver().asSourceString();
            message += String.format("Try changing `%1$s` to `%1$s.getApplicationContext()`",
                    qualifier);
            fix = fix()
                    .name("Add getApplicationContext()")
                    .replace().text(qualifier).with(qualifier + ".getApplicationContext()")
                    .build();
        } else {
            String qualifier = call.getMethodName();
            message += String.format("Try changing `%1$s` to `getApplicationContext().%1$s`",
                    qualifier);
            fix = fix()
                    .name("Add getApplicationContext()")
                    .replace().text(qualifier).with("getApplicationContext()." + qualifier)
                    .build();
        }

        context.report(issue, call, context.getLocation(call), message, fix);
    }

    private static boolean isClipboard(@NonNull String cls) {
        return cls.equals("android.content.ClipboardManager")
                || cls.equals("android.text.ClipboardManager");
    }

    private static String stripPackage(@NonNull String fqcn) {
        int index = fqcn.lastIndexOf('.');
        if (index != -1) {
            fqcn = fqcn.substring(index + 1);
        }

        return fqcn;
    }

    @Nullable
    private static String getExpectedType(@Nullable String value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case "ACCESSIBILITY_SERVICE": return "android.view.accessibility.AccessibilityManager";
            case "ACCOUNT_SERVICE": return "android.accounts.AccountManager";
            case "ACTIVITY_SERVICE": return "android.app.ActivityManager";
            case "ALARM_SERVICE": return "android.app.AlarmManager";
            case "APPWIDGET_SERVICE": return "android.appwidget.AppWidgetManager";
            case "APP_OPS_SERVICE": return "android.app.AppOpsManager";
            case "AUDIO_SERVICE": return "android.media.AudioManager";
            case "BATTERY_SERVICE": return "android.os.BatteryManager";
            case "BLUETOOTH_SERVICE": return "android.bluetooth.BluetoothManager";
            case "CAMERA_SERVICE": return "android.hardware.camera2.CameraManager";
            case "CAPTIONING_SERVICE": return "android.view.accessibility.CaptioningManager";
            case "CARRIER_CONFIG_SERVICE": return "android.telephony.CarrierConfigManager";
            // also allow @Deprecated android.content.ClipboardManager, see isClipboard
            case "CLIPBOARD_SERVICE": return "android.text.ClipboardManager";
            case "COMPANION_DEVICE_SERVICE": return "android.companion.CompanionDeviceManager";
            case "CONNECTIVITY_SERVICE": return "android.net.ConnectivityManager";
            case "CONSUMER_IR_SERVICE": return "android.hardware.ConsumerIrManager";
            case "DEVICE_POLICY_SERVICE": return "android.app.admin.DevicePolicyManager";
            case "DISPLAY_SERVICE": return "android.hardware.display.DisplayManager";
            case "DOWNLOAD_SERVICE": return "android.app.DownloadManager";
            case "DROPBOX_SERVICE": return "android.os.DropBoxManager";
            case "FINGERPRINT_SERVICE": return "android.hardware.fingerprint.FingerprintManager";
            case "HARDWARE_PROPERTIES_SERVICE": return "android.os.HardwarePropertiesManager";
            case "INPUT_METHOD_SERVICE": return "android.view.inputmethod.InputMethodManager";
            case "INPUT_SERVICE": return "android.hardware.input.InputManager";
            case "IPSEC_SERVICE": return "android.net.IpSecManager";
            case "JOB_SCHEDULER_SERVICE": return "android.app.job.JobScheduler";
            case "KEYGUARD_SERVICE": return "android.app.KeyguardManager";
            case "LAUNCHER_APPS_SERVICE": return "android.content.pm.LauncherApps";
            case "LAYOUT_INFLATER_SERVICE": return "android.view.LayoutInflater";
            case "LOCATION_SERVICE": return "android.location.LocationManager";
            case "MEDIA_PROJECTION_SERVICE": return "android.media.projection.MediaProjectionManager";
            case "MEDIA_ROUTER_SERVICE": return "android.media.MediaRouter";
            case "MEDIA_SESSION_SERVICE": return "android.media.session.MediaSessionManager";
            case "MIDI_SERVICE": return "android.media.midi.MidiManager";
            case "NETWORK_STATS_SERVICE": return "android.app.usage.NetworkStatsManager";
            case "NFC_SERVICE": return "android.nfc.NfcManager";
            case "NOTIFICATION_SERVICE": return "android.app.NotificationManager";
            case "NSD_SERVICE": return "android.net.nsd.NsdManager";
            case "POWER_SERVICE": return "android.os.PowerManager";
            case "PRINT_SERVICE": return "android.print.PrintManager";
            case "RESTRICTIONS_SERVICE": return "android.content.RestrictionsManager";
            case "SEARCH_SERVICE": return "android.app.SearchManager";
            case "SENSOR_SERVICE": return "android.hardware.SensorManager";
            case "SHORTCUT_SERVICE": return "android.content.pm.ShortcutManager";
            case "STORAGE_SERVICE": return "android.os.storage.StorageManager";
            case "STORAGE_STATS_SERVICE": return "android.app.usage.StorageStatsManager";
            case "SYSTEM_HEALTH_SERVICE": return "android.os.health.SystemHealthManager";
            case "TELECOM_SERVICE": return "android.telecom.TelecomManager";
            case "TELEPHONY_SERVICE": return "android.telephony.TelephonyManager";
            case "TELEPHONY_SUBSCRIPTION_SERVICE": return "android.telephony.SubscriptionManager";
            case "TEXT_CLASSIFICATION_SERVICE": return "android.view.textclassifier.TextClassificationManager";
            case "TEXT_SERVICES_MANAGER_SERVICE": return "android.view.textservice.TextServicesManager";
            case "TV_INPUT_SERVICE": return "android.media.tv.TvInputManager";
            case "UI_MODE_SERVICE": return "android.app.UiModeManager";
            case "USAGE_STATS_SERVICE": return "android.app.usage.UsageStatsManager";
            case "USB_SERVICE": return "android.hardware.usb.UsbManager";
            case "USER_SERVICE": return "android.os.UserManager";
            case "VIBRATOR_SERVICE": return "android.os.Vibrator";
            case "WALLPAPER_SERVICE": return "android.app.WallpaperManager";
            case "WIFI_AWARE_SERVICE": return "android.net.wifi.aware.WifiAwareManager";
            case "WIFI_P2P_SERVICE": return "android.net.wifi.p2p.WifiP2pManager";
            case "WIFI_SERVICE": return "android.net.wifi.WifiManager";
            case "WINDOW_SERVICE": return "android.view.WindowManager";
            default: return null;
        }
    }
}
