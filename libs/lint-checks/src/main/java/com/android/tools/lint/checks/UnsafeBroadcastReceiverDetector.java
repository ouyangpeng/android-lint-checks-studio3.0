/*
 * Copyright (C) 2015 The Android Open Source Project
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
import static com.android.SdkConstants.ATTR_PERMISSION;
import static com.android.SdkConstants.CLASS_BROADCASTRECEIVER;
import static com.android.SdkConstants.CLASS_CONTEXT;
import static com.android.SdkConstants.CLASS_INTENT;
import static com.android.SdkConstants.TAG_ACTION;
import static com.android.SdkConstants.TAG_APPLICATION;
import static com.android.SdkConstants.TAG_INTENT_FILTER;
import static com.android.SdkConstants.TAG_RECEIVER;
import static com.android.utils.XmlUtils.getFirstSubTagByName;
import static com.android.utils.XmlUtils.getSubTagsByName;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Detector.XmlScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.XmlUtils;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.USimpleNameReferenceExpression;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class UnsafeBroadcastReceiverDetector extends Detector
        implements UastScanner, XmlScanner {

    // TODO: Use the new merged manifest model


    /* Description of check implementations:
     *
     * UnsafeProtectedBroadcastReceiver check
     *
     * If a receiver is declared in the application manifest that has an intent-filter
     * with an action string that matches a protected-broadcast action string,
     * then if that receiver has an onReceive method, ensure that the method calls
     * getAction at least once.
     *
     * With this check alone, false positives will occur if the onReceive method
     * passes the received intent to another method that calls getAction.
     * We look for any calls to aload_2 within the method bytecode, which could
     * indicate loading the inputted intent onto the stack to use in a call
     * to another method. In those cases, still report the issue, but
     * report in the description that the finding may be a false positive.
     * An alternative implementation option would be to omit reporting the issue
     * at all when a call to aload_2 exists.
     *
     * UnprotectedSMSBroadcastReceiver check
     *
     * If a receiver is declared in AndroidManifest that has an intent-filter
     * with action string SMS_DELIVER or SMS_RECEIVED, ensure that the
     * receiver requires callers to have the BROADCAST_SMS permission.
     *
     * It is possible that the receiver may check the sender's permission by
     * calling checkCallingPermission, which could cause a false positive.
     * However, application developers should still be encouraged to declare
     * the permission requirement in the manifest where it can be easily
     * audited.
     *
     * Future work: Add checks for other action strings that should require
     * particular permissions be checked, such as
     * android.provider.Telephony.WAP_PUSH_DELIVER
     *
     * Note that neither of these checks address receivers dynamically created at runtime,
     * only ones that are declared in the application manifest.
     */

    public static final Issue ACTION_STRING = Issue.create(
            "UnsafeProtectedBroadcastReceiver",
            "Unsafe Protected BroadcastReceiver",
            "BroadcastReceivers that declare an intent-filter for a protected-broadcast action " +
            "string must check that the received intent's action string matches the expected " +
            "value, otherwise it is possible for malicious actors to spoof intents.",
            Category.SECURITY,
            6,
            Severity.WARNING,
            new Implementation(UnsafeBroadcastReceiverDetector.class,
                    EnumSet.of(Scope.MANIFEST, Scope.JAVA_FILE),
                    Scope.JAVA_FILE_SCOPE));

    public static final Issue BROADCAST_SMS = Issue.create(
            "UnprotectedSMSBroadcastReceiver",
            "Unprotected SMS BroadcastReceiver",
            "BroadcastReceivers that declare an intent-filter for SMS_DELIVER or " +
            "SMS_RECEIVED must ensure that the caller has the BROADCAST_SMS permission, " +
            "otherwise it is possible for malicious actors to spoof intents.",
            Category.SECURITY,
            6,
            Severity.WARNING,
            new Implementation(UnsafeBroadcastReceiverDetector.class,
                    Scope.MANIFEST_SCOPE));

    /* List of protected broadcast strings.
     * Protected broadcast strings are defined by <protected-broadcast> entries in the
     * manifest of system-level components or applications.
     * The below list is copied from frameworks/base/core/res/AndroidManifest.xml
     * and packages/services/Telephony/AndroidManifest.xml .
     * It should be periodically updated. This list will likely not be complete, since
     * protected-broadcast entries can be defined elsewhere, but should address
     * most situations.
     */
    @VisibleForTesting
    static boolean isProtectedBroadcast(@NonNull String actionName) {
        switch (actionName) {
            case "EventConditionProvider.EVALUATE":
            case "ScheduleConditionProvider.EVALUATE":
            case "action.cne.started":
            case "android.accounts.LOGIN_ACCOUNTS_CHANGED":
            case "android.app.action.ACTION_PASSWORD_CHANGED":
            case "android.app.action.ACTION_PASSWORD_EXPIRING":
            case "android.app.action.ACTION_PASSWORD_FAILED":
            case "android.app.action.ACTION_PASSWORD_SUCCEEDED":
            case "android.app.action.BUGREPORT_FAILED":
            case "android.app.action.BUGREPORT_SHARE":
            case "android.app.action.BUGREPORT_SHARING_DECLINED":
            case "android.app.action.CHOOSE_PRIVATE_KEY_ALIAS":
            case "android.app.action.DEVICE_ADMIN_DISABLED":
            case "android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED":
            case "android.app.action.DEVICE_ADMIN_ENABLED":
            case "android.app.action.DEVICE_OWNER_CHANGED":
            case "android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED":
            case "android.app.action.ENTER_CAR_MODE":
            case "android.app.action.ENTER_DESK_MODE":
            case "android.app.action.EXIT_CAR_MODE":
            case "android.app.action.EXIT_DESK_MODE":
            case "android.app.action.INTERRUPTION_FILTER_CHANGED":
            case "android.app.action.INTERRUPTION_FILTER_CHANGED_INTERNAL":
            case "android.app.action.LOCK_TASK_ENTERING":
            case "android.app.action.LOCK_TASK_EXITING":
            case "android.app.action.NEXT_ALARM_CLOCK_CHANGED":
            case "android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED":
            case "android.app.action.NOTIFICATION_POLICY_CHANGED":
            case "android.app.action.NOTIFY_PENDING_SYSTEM_UPDATE":
            case "android.app.action.SECURITY_LOGS_AVAILABLE":
            case "android.app.action.SYSTEM_UPDATE_POLICY_CHANGED":
            case "android.app.backup.intent.CLEAR":
            case "android.app.backup.intent.INIT":
            case "android.app.backup.intent.RUN":
            case "android.appwidget.action.APPWIDGET_DELETED":
            case "android.appwidget.action.APPWIDGET_DISABLED":
            case "android.appwidget.action.APPWIDGET_ENABLED":
            case "android.appwidget.action.APPWIDGET_HOST_RESTORED":
            case "android.appwidget.action.APPWIDGET_RESTORED":
            case "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS":
            case "android.bluetooth.a2dp-sink.profile.action.AUDIO_CONFIG_CHANGED":
            case "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED":
            case "android.bluetooth.a2dp-sink.profile.action.PLAYING_STATE_CHANGED":
            case "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED":
            case "android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED":
            case "android.bluetooth.adapter.action.BLE_ACL_CONNECTED":
            case "android.bluetooth.adapter.action.BLE_ACL_DISCONNECTED":
            case "android.bluetooth.adapter.action.BLE_STATE_CHANGED":
            case "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED":
            case "android.bluetooth.adapter.action.DISCOVERY_FINISHED":
            case "android.bluetooth.adapter.action.DISCOVERY_STARTED":
            case "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED":
            case "android.bluetooth.adapter.action.SCAN_MODE_CHANGED":
            case "android.bluetooth.adapter.action.STATE_CHANGED":
            case "android.bluetooth.avrcp-controller.profile.action.CONNECTION_STATE_CHANGED":
            case "android.bluetooth.device.action.ACL_CONNECTED":
            case "android.bluetooth.device.action.ACL_DISCONNECTED":
            case "android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED":
            case "android.bluetooth.device.action.ALIAS_CHANGED":
            case "android.bluetooth.device.action.BOND_STATE_CHANGED":
            case "android.bluetooth.device.action.CLASS_CHANGED":
            case "android.bluetooth.device.action.CONNECTION_ACCESS_CANCEL":
            case "android.bluetooth.device.action.CONNECTION_ACCESS_REPLY":
            case "android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST":
            case "android.bluetooth.device.action.DISAPPEARED":
            case "android.bluetooth.device.action.FOUND":
            case "android.bluetooth.device.action.MAS_INSTANCE":
            case "android.bluetooth.device.action.NAME_CHANGED":
            case "android.bluetooth.device.action.NAME_FAILED":
            case "android.bluetooth.device.action.PAIRING_CANCEL":
            case "android.bluetooth.device.action.PAIRING_REQUEST":
            case "android.bluetooth.device.action.SDP_RECORD":
            case "android.bluetooth.device.action.UUID":
            case "android.bluetooth.devicepicker.action.DEVICE_SELECTED":
            case "android.bluetooth.devicepicker.action.LAUNCH":
            case "android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT":
            case "android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED":
            case "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED":
            case "android.bluetooth.headsetclient.profile.action.AG_CALL_CHANGED":
            case "android.bluetooth.headsetclient.profile.action.AG_EVENT":
            case "android.bluetooth.headsetclient.profile.action.AUDIO_STATE_CHANGED":
            case "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED":
            case "android.bluetooth.headsetclient.profile.action.LAST_VTAG":
            case "android.bluetooth.headsetclient.profile.action.RESULT":
            case "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED":
            case "android.bluetooth.input.profile.action.HANDSHAKE":
            case "android.bluetooth.input.profile.action.PROTOCOL_MODE_CHANGED":
            case "android.bluetooth.input.profile.action.REPORT":
            case "android.bluetooth.input.profile.action.VIRTUAL_UNPLUG_STATUS":
            case "android.bluetooth.intent.DISCOVERABLE_TIMEOUT":
            case "android.bluetooth.map.profile.action.CONNECTION_STATE_CHANGED":
            case "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED":
            case "android.bluetooth.pbap.intent.action.PBAP_STATE_CHANGED":
            case "android.btopp.intent.action.CONFIRM":
            case "android.btopp.intent.action.HIDE":
            case "android.btopp.intent.action.HIDE_COMPLETE":
            case "android.btopp.intent.action.INCOMING_FILE_NOTIFICATION":
            case "android.btopp.intent.action.LIST":
            case "android.btopp.intent.action.OPEN":
            case "android.btopp.intent.action.OPEN_INBOUND":
            case "android.btopp.intent.action.OPEN_OUTBOUND":
            case "android.btopp.intent.action.RETRY":
            case "android.btopp.intent.action.STOP_HANDOVER_TRANSFER":
            case "android.btopp.intent.action.TRANSFER_COMPLETE":
            case "android.btopp.intent.action.USER_CONFIRMATION_TIMEOUT":
            case "android.btopp.intent.action.WHITELIST_DEVICE":
            case "android.content.jobscheduler.JOB_DEADLINE_EXPIRED":
            case "android.content.jobscheduler.JOB_DELAY_EXPIRED":
            case "android.content.syncmanager.SYNC_ALARM":
            case "android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED":
            case "android.hardware.usb.action.USB_ACCESSORY_ATTACHED":
            case "android.hardware.usb.action.USB_ACCESSORY_DETACHED":
            case "android.hardware.usb.action.USB_DEVICE_ATTACHED":
            case "android.hardware.usb.action.USB_DEVICE_DETACHED":
            case "android.hardware.usb.action.USB_PORT_CHANGED":
            case "android.hardware.usb.action.USB_STATE":
            case "android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED":
            case "android.intent.action.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED":
            case "android.intent.action.ACTION_DEFAULT_SUBSCRIPTION_CHANGED":
            case "android.telephony.action.DEFAULT_SMS_SUBSCRIPTION_CHANGED":
            case "android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED":
            case "android.intent.action.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED":
            case "android.intent.action.ACTION_IDLE_MAINTENANCE_END":
            case "android.intent.action.ACTION_IDLE_MAINTENANCE_START":
            case "android.intent.action.ACTION_POWER_CONNECTED":
            case "android.intent.action.ACTION_POWER_DISCONNECTED":
            case "android.intent.action.ACTION_RADIO_OFF":
            case "android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE":
            case "android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED":
            case "android.intent.action.ACTION_SHUTDOWN":
            case "android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE":
            case "android.intent.action.ACTION_SUBINFO_RECORD_UPDATED":
            case "android.intent.action.ACTION_UNSOL_RESPONSE_OEM_HOOK_RAW":
            case "android.intent.action.ADVANCED_SETTINGS":
            case "android.intent.action.AIRPLANE_MODE":
            case "android.intent.action.ANR":
            case "android.intent.action.ANY_DATA_STATE":
            case "android.intent.action.APPLICATION_RESTRICTIONS_CHANGED":
            case "android.intent.action.BATTERY_CHANGED":
            case "android.intent.action.BATTERY_LOW":
            case "android.intent.action.BATTERY_OKAY":
            case "android.intent.action.BOOT_COMPLETED":
            case "android.intent.action.CALL":
            case "android.intent.action.CALL_PRIVILEGED":
            case "android.intent.action.CHARGING":
            case "android.intent.action.CLEAR_DNS_CACHE":
            case "android.intent.action.CONFIGURATION_CHANGED":
            case "android.intent.action.CONTENT_CHANGED":
            case "android.intent.action.DATE_CHANGED":
            case "android.intent.action.DEVICE_STORAGE_FULL":
            case "android.intent.action.DEVICE_STORAGE_LOW":
            case "android.intent.action.DEVICE_STORAGE_NOT_FULL":
            case "android.intent.action.DEVICE_STORAGE_OK":
            case "android.intent.action.DISCHARGING":
            case "android.intent.action.DOCK_EVENT":
            case "android.intent.action.DREAMING_STARTED":
            case "android.intent.action.DREAMING_STOPPED":
            case "android.intent.action.DROPBOX_ENTRY_ADDED":
            case "android.intent.action.DYNAMIC_SENSOR_CHANGED":
            case "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE":
            case "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE":
            case "android.intent.action.GLOBAL_BUTTON":
            case "android.intent.action.HDMI_PLUGGED":
            case "android.intent.action.HEADSET_PLUG":
            case "android.intent.action.INPUT_METHOD_CHANGED":
            case "android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION":
            case "android.intent.action.LOCALE_CHANGED":
            case "android.intent.action.LOCKED_BOOT_COMPLETED":
            case "android.intent.action.MANAGED_PROFILE_ADDED":
            case "android.intent.action.MANAGED_PROFILE_AVAILABLE":
            case "android.intent.action.MANAGED_PROFILE_REMOVED":
            case "android.intent.action.MANAGED_PROFILE_UNAVAILABLE":
            case "android.intent.action.MANAGED_PROFILE_UNLOCKED":
            case "android.intent.action.MASTER_CLEAR_NOTIFICATION":
            case "android.intent.action.MEDIA_BAD_REMOVAL":
            case "android.intent.action.MEDIA_CHECKING":
            case "android.intent.action.MEDIA_EJECT":
            case "android.intent.action.MEDIA_MOUNTED":
            case "android.intent.action.MEDIA_NOFS":
            case "android.intent.action.MEDIA_REMOVED":
            case "android.intent.action.MEDIA_RESOURCE_GRANTED":
            case "android.intent.action.MEDIA_SHARED":
            case "android.intent.action.MEDIA_UNMOUNTABLE":
            case "android.intent.action.MEDIA_UNMOUNTED":
            case "android.intent.action.MEDIA_UNSHARED":
            case "android.intent.action.MY_PACKAGE_REPLACED":
            case "android.intent.action.NEW_OUTGOING_CALL":
            case "android.intent.action.PACKAGES_SUSPENDED":
            case "android.intent.action.PACKAGES_UNSUSPENDED":
            case "android.intent.action.PACKAGE_ADDED":
            case "android.intent.action.PACKAGE_CHANGED":
            case "android.intent.action.PACKAGE_DATA_CLEARED":
            case "android.intent.action.PACKAGE_FIRST_LAUNCH":
            case "android.intent.action.PACKAGE_FULLY_REMOVED":
            case "android.intent.action.PACKAGE_INSTALL":
            case "android.intent.action.PACKAGE_NEEDS_VERIFICATION":
            case "android.intent.action.PACKAGE_REMOVED":
            case "android.intent.action.PACKAGE_REPLACED":
            case "android.intent.action.PACKAGE_RESTARTED":
            case "android.intent.action.PACKAGE_VERIFIED":
            case "android.intent.action.PERMISSION_RESPONSE_RECEIVED":
            case "android.intent.action.PHONE_STATE":
            case "android.intent.action.PRECISE_CALL_STATE":
            case "android.intent.action.PRECISE_DATA_CONNECTION_STATE_CHANGED":
            case "android.intent.action.PRE_BOOT_COMPLETED":
            case "android.intent.action.PROXY_CHANGE":
            case "android.intent.action.QUERY_PACKAGE_RESTART":
            case "android.intent.action.REBOOT":
            case "android.intent.action.REQUEST_PERMISSION":
            case "android.intent.action.SCREEN_OFF":
            case "android.intent.action.SCREEN_ON":
            case "android.intent.action.SUBSCRIPTION_PHONE_STATE":
            case "android.intent.action.SUB_DEFAULT_CHANGED":
            case "android.intent.action.THERMAL_EVENT":
            case "android.intent.action.TIMEZONE_CHANGED":
            case "android.intent.action.TIME_SET":
            case "android.intent.action.TIME_TICK":
            case "android.intent.action.TWILIGHT_CHANGED":
            case "android.intent.action.UID_REMOVED":
            case "android.intent.action.USER_ADDED":
            case "android.intent.action.USER_BACKGROUND":
            case "android.intent.action.USER_FOREGROUND":
            case "android.intent.action.USER_INFO_CHANGED":
            case "android.intent.action.USER_INITIALIZE":
            case "android.intent.action.USER_PRESENT":
            case "android.intent.action.USER_REMOVED":
            case "android.intent.action.USER_STARTED":
            case "android.intent.action.USER_STARTING":
            case "android.intent.action.USER_STOPPED":
            case "android.intent.action.USER_STOPPING":
            case "android.intent.action.USER_SWITCHED":
            case "android.intent.action.USER_UNLOCKED":
            case "android.intent.action.WALLPAPER_CHANGED":
            case "android.intent.action.internal_sim_state_changed":
            case "android.internal.policy.action.BURN_IN_PROTECTION":
            case "android.location.GPS_ENABLED_CHANGE":
            case "android.location.GPS_FIX_CHANGE":
            case "android.location.MODE_CHANGED":
            case "android.location.PROVIDERS_CHANGED":
            case "android.media.ACTION_SCO_AUDIO_STATE_UPDATED":
            case "android.media.AUDIO_BECOMING_NOISY":
            case "android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION":
            case "android.media.MASTER_MONO_CHANGED_ACTION":
            case "android.media.MASTER_MUTE_CHANGED_ACTION":
            case "android.media.MASTER_VOLUME_CHANGED_ACTION":
            case "android.media.RINGER_MODE_CHANGED":
            case "android.media.SCO_AUDIO_STATE_CHANGED":
            case "android.media.STREAM_DEVICES_CHANGED_ACTION":
            case "android.media.STREAM_MUTE_CHANGED_ACTION":
            case "android.media.VIBRATE_SETTING_CHANGED":
            case "android.media.VOLUME_CHANGED_ACTION":
            case "android.media.action.HDMI_AUDIO_PLUG":
            case "android.net.ConnectivityService.action.PKT_CNT_SAMPLE_INTERVAL_ELAPSED":
            case "android.net.conn.BACKGROUND_DATA_SETTING_CHANGED":
            case "android.net.conn.CAPTIVE_PORTAL":
            case "android.net.conn.CAPTIVE_PORTAL_TEST_COMPLETED":
            case "android.net.conn.CONNECTIVITY_CHANGE":
            case "android.net.conn.CONNECTIVITY_CHANGE_IMMEDIATE":
            case "android.net.conn.CONNECTIVITY_CHANGE_SUPL":
            case "android.net.conn.DATA_ACTIVITY_CHANGE":
            case "android.net.conn.INET_CONDITION_ACTION":
            case "android.net.conn.NETWORK_CONDITIONS_MEASURED":
            case "android.net.conn.RESTRICT_BACKGROUND_CHANGED":
            case "android.net.conn.TETHER_STATE_CHANGED":
            case "android.net.nsd.STATE_CHANGED":
            case "android.net.proxy.PAC_REFRESH":
            case "android.net.scoring.SCORER_CHANGED":
            case "android.net.scoring.SCORE_NETWORKS":
            case "android.net.sip.SIP_SERVICE_UP":
            case "android.net.wifi.CONFIGURED_NETWORKS_CHANGE":
            case "android.net.wifi.LINK_CONFIGURATION_CHANGED":
            case "android.net.wifi.PASSPOINT_ICON_RECEIVED":
            case "android.net.wifi.RSSI_CHANGED":
            case "android.net.wifi.SCAN_RESULTS":
            case "android.net.wifi.STATE_CHANGE":
            case "android.net.wifi.WIFI_AP_STATE_CHANGED":
            case "android.net.wifi.WIFI_CREDENTIAL_CHANGED":
            case "android.net.wifi.WIFI_SCAN_AVAILABLE":
            case "android.net.wifi.WIFI_STATE_CHANGED":
            case "android.net.wifi.p2p.CONNECTION_STATE_CHANGE":
            case "android.net.wifi.p2p.DISCOVERY_STATE_CHANGE":
            case "android.net.wifi.p2p.PEERS_CHANGED":
            case "android.net.wifi.p2p.PERSISTENT_GROUPS_CHANGED":
            case "android.net.wifi.p2p.STATE_CHANGED":
            case "android.net.wifi.p2p.THIS_DEVICE_CHANGED":
            case "android.net.wifi.supplicant.CONNECTION_CHANGE":
            case "android.net.wifi.supplicant.STATE_CHANGE":
            case "android.nfc.action.ADAPTER_STATE_CHANGED":
            case "android.nfc.action.TRANSACTION_DETECTED":
            case "android.nfc.handover.intent.action.HANDOVER_SEND":
            case "android.nfc.handover.intent.action.HANDOVER_SEND_MULTIPLE":
            case "android.nfc.handover.intent.action.HANDOVER_STARTED":
            case "android.nfc.handover.intent.action.TRANSFER_DONE":
            case "android.nfc.handover.intent.action.TRANSFER_PROGRESS":
            case "android.os.UpdateLock.UPDATE_LOCK_CHANGED":
            case "android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED":
            case "android.os.action.CHARGING":
            case "android.os.action.DEVICE_IDLE_MODE_CHANGED":
            case "android.os.action.DISCHARGING":
            case "android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED":
            case "android.os.action.POWER_SAVE_MODE_CHANGED":
            case "android.os.action.POWER_SAVE_MODE_CHANGED_INTERNAL":
            case "android.os.action.POWER_SAVE_MODE_CHANGING":
            case "android.os.action.POWER_SAVE_TEMP_WHITELIST_CHANGED":
            case "android.os.action.POWER_SAVE_WHITELIST_CHANGED":
            case "android.os.action.SCREEN_BRIGHTNESS_BOOST_CHANGED":
            case "android.os.action.SETTING_RESTORED":
            case "android.os.storage.action.DISK_SCANNED":
            case "android.os.storage.action.VOLUME_STATE_CHANGED":
            case "android.permission.CLEAR_APP_GRANTED_URI_PERMISSIONS":
            case "android.permission.GET_APP_GRANTED_URI_PERMISSIONS":
            case "android.provider.Telephony.MMS_DOWNLOADED":
            case "android.provider.action.DEFAULT_SMS_PACKAGE_CHANGED":
            case "android.search.action.SEARCHABLES_CHANGED":
            case "android.security.STORAGE_CHANGED":
            case "android.telecom.action.DEFAULT_DIALER_CHANGED":
            case "android.telecom.action.PHONE_ACCOUNT_REGISTERED":
            case "android.telecom.action.PHONE_ACCOUNT_UNREGISTERED":
            case "android.telecom.action.SHOW_MISSED_CALLS_NOTIFICATION":
            case "android.telephony.action.CARRIER_CONFIG_CHANGED":
            case "com.android.bluetooth.btservice.action.ALARM_WAKEUP":
            case "com.android.bluetooth.map.USER_CONFIRM_TIMEOUT":
            case "com.android.bluetooth.pbap.authcancelled":
            case "com.android.bluetooth.pbap.authchall":
            case "com.android.bluetooth.pbap.authresponse":
            case "com.android.bluetooth.pbap.userconfirmtimeout":
            case "com.android.ims.IMS_INCOMING_CALL":
            case "com.android.ims.IMS_SERVICE_UP":
            case "com.android.ims.internal.uce.UCE_SERVICE_UP":
            case "com.android.intent.action.IMS_CONFIG_CHANGED":
            case "com.android.intent.action.IMS_FEATURE_CHANGED":
            case "com.android.internal.location.ALARM_TIMEOUT":
            case "com.android.internal.location.ALARM_WAKEUP":
            case "com.android.nfc.action.LLCP_DOWN":
            case "com.android.nfc.action.LLCP_UP":
            case "com.android.nfc.cardemulation.action.CLOSE_TAP_DIALOG":
            case "com.android.nfc.handover.action.ALLOW_CONNECT":
            case "com.android.nfc.handover.action.DENY_CONNECT":
            case "com.android.nfc_extras.action.AID_SELECTED":
            case "com.android.nfc_extras.action.RF_FIELD_OFF_DETECTED":
            case "com.android.nfc_extras.action.RF_FIELD_ON_DETECTED":
            case "com.android.phone.SIP_ADD_PHONE":
            case "com.android.phone.SIP_CALL_OPTION_CHANGED":
            case "com.android.phone.SIP_INCOMING_CALL":
            case "com.android.phone.SIP_REMOVE_PHONE":
            case "com.android.server.ACTION_EXPIRED_PASSWORD_NOTIFICATION":
            case "com.android.server.ACTION_TRIGGER_IDLE":
            case "com.android.server.NetworkTimeUpdateService.action.POLL":
            case "com.android.server.Wifi.action.TOGGLE_PNO":
            case "com.android.server.WifiManager.action.DELAYED_DRIVER_STOP":
            case "com.android.server.WifiManager.action.DEVICE_IDLE":
            case "com.android.server.WifiManager.action.START_PNO":
            case "com.android.server.WifiManager.action.START_SCAN":
            case "com.android.server.action.NETWORK_STATS_POLL":
            case "com.android.server.action.NETWORK_STATS_UPDATED":
            case "com.android.server.action.REMOTE_BUGREPORT_SHARING_ACCEPTED":
            case "com.android.server.action.REMOTE_BUGREPORT_SHARING_DECLINED":
            case "com.android.server.action.RESET_TWILIGHT_AUTO":
            case "com.android.server.action.UPDATE_TWILIGHT_STATE":
            case "com.android.server.am.DELETE_DUMPHEAP":
            case "com.android.server.connectivityservice.CONNECTED_TO_PROVISIONING_NETWORK_ACTION":
            case "com.android.server.device_idle.STEP_IDLE_STATE":
            case "com.android.server.device_idle.STEP_LIGHT_IDLE_STATE":
            case "com.android.server.fingerprint.ACTION_LOCKOUT_RESET":
            case "com.android.server.net.action.SNOOZE_WARNING":
            case "com.android.server.notification.CountdownConditionProvider":
            case "com.android.server.pm.DISABLE_QUIET_MODE_AFTER_UNLOCK":
            case "com.android.server.telecom.intent.action.CALLS_ADD_ENTRY":
            case "com.android.server.usb.ACTION_OPEN_IN_APPS":
            case "com.android.settings.location.MODE_CHANGING":
            case "com.android.sync.SYNC_CONN_STATUS_CHANGED":
            case "intent.action.ACTION_RF_BAND_INFO":
            case "wifi_scan_available":
                return true;
            default:
                return false;
        }
    }

    private Set<String> mReceiversWithProtectedBroadcastIntentFilter = null;

    public UnsafeBroadcastReceiverDetector() {
    }

    // ---- Implements XmlScanner ----

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(TAG_RECEIVER);
    }

    @Override
    public void visitElement(@NonNull XmlContext context,
            @NonNull Element element) {
        String tag = element.getTagName();
        if (TAG_RECEIVER.equals(tag)) {
            String name = LintUtils.resolveManifestName(element);
            String permission = element.getAttributeNS(ANDROID_URI, ATTR_PERMISSION);
            // If no permission attribute, then if any exists at the application
            // element, it applies
            if (permission == null || permission.isEmpty()) {
                Element parent = (Element) element.getParentNode();
                permission = parent.getAttributeNS(ANDROID_URI, ATTR_PERMISSION);
            }
            Element filter = getFirstSubTagByName(element, TAG_INTENT_FILTER);
            if (filter != null) {
                for (Element action : getSubTagsByName(filter, TAG_ACTION)) {
                    String actionName = action.getAttributeNS(
                            ANDROID_URI, ATTR_NAME);
                    if (("android.provider.Telephony.SMS_DELIVER".equals(actionName) ||
                            "android.provider.Telephony.SMS_RECEIVED".
                                equals(actionName)) &&
                            !"android.permission.BROADCAST_SMS".equals(permission)) {
                        LintFix fix = fix().set(ANDROID_URI, ATTR_PERMISSION,
                                "android.permission.BROADCAST_SMS").build();
                        context.report(
                                BROADCAST_SMS,
                                element,
                                context.getLocation(element),
                                "BroadcastReceivers that declare an intent-filter for " +
                                "SMS_DELIVER or SMS_RECEIVED must ensure that the " +
                                "caller has the BROADCAST_SMS permission, otherwise it " +
                                "is possible for malicious actors to spoof intents.", fix);
                    }
                    else if (isProtectedBroadcast(actionName)) {
                        if (mReceiversWithProtectedBroadcastIntentFilter == null) {
                            mReceiversWithProtectedBroadcastIntentFilter = Sets.newHashSet();
                        }
                        mReceiversWithProtectedBroadcastIntentFilter.add(name);
                    }
                }
            }
        }
    }

    private Set<String> getReceiversWithProtectedBroadcastIntentFilter(@NonNull Context context) {
        if (mReceiversWithProtectedBroadcastIntentFilter == null) {
            mReceiversWithProtectedBroadcastIntentFilter = Sets.newHashSet();
            if (!context.getScope().contains(Scope.MANIFEST)) {
                // Compute from merged manifest
                Project mainProject = context.getMainProject();
                Document mergedManifest = mainProject.getMergedManifest();
                if (mergedManifest != null &&
                        mergedManifest.getDocumentElement() != null) {
                    Element application = getFirstSubTagByName(
                            mergedManifest.getDocumentElement(), TAG_APPLICATION);
                    if (application != null) {
                        for (Element element : XmlUtils.getSubTags(application)) {
                            if (TAG_RECEIVER.equals(element.getTagName())) {
                                Element filter = getFirstSubTagByName(element, TAG_INTENT_FILTER);
                                if (filter != null) {
                                    for (Element action : getSubTagsByName(filter, TAG_ACTION)) {
                                        String actionName = action.getAttributeNS(
                                                ANDROID_URI, ATTR_NAME);
                                        if (isProtectedBroadcast(actionName)) {
                                            String name = LintUtils.resolveManifestName(element);
                                            mReceiversWithProtectedBroadcastIntentFilter.add(name);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return mReceiversWithProtectedBroadcastIntentFilter;
    }

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Collections.singletonList(CLASS_BROADCASTRECEIVER);
    }

    @Override
    public void visitClass(@NonNull JavaContext context, @NonNull UClass declaration) {
        String name = declaration.getName();
        if (name == null) {
            // anonymous classes can't be the ones referenced in the manifest
            return;
        }
        String qualifiedName = declaration.getQualifiedName();
        if (qualifiedName == null) {
            return;
        }
        if (!getReceiversWithProtectedBroadcastIntentFilter(context).contains(qualifiedName)) {
            return;
        }
        JavaEvaluator evaluator = context.getEvaluator();
        for (PsiMethod method : declaration.findMethodsByName("onReceive", false)) {
            if (evaluator.parametersMatch(method, CLASS_CONTEXT, CLASS_INTENT)) {
                checkOnReceive(context, method);
            }
        }
    }

    private static void checkOnReceive(@NonNull JavaContext context,
            @NonNull PsiMethod method) {
        // Search for call to getAction but also search for references to aload_2,
        // which indicates that the method is making use of the received intent in
        // some way.
        //
        // If the onReceive method doesn't call getAction but does make use of
        // the received intent, it is possible that it is passing it to another
        // method that might be performing the getAction check, so we warn that the
        // finding may be a false positive. (An alternative option would be to not
        // report a finding at all in this case.)
        PsiParameter parameter = method.getParameterList().getParameters()[1];
        OnReceiveVisitor visitor = new OnReceiveVisitor(context.getEvaluator(), parameter);
        context.getUastContext().getMethodBody(method).accept(visitor);
        if (!visitor.getCallsGetAction()) {
            String report;
            if (!visitor.getUsesIntent()) {
                report = "This broadcast receiver declares an intent-filter for a protected " +
                        "broadcast action string, which can only be sent by the system, " +
                        "not third-party applications. However, the receiver's onReceive " +
                        "method does not appear to call getAction to ensure that the " +
                        "received Intent's action string matches the expected value, " +
                        "potentially making it possible for another actor to send a " +
                        "spoofed intent with no action string or a different action " +
                        "string and cause undesired behavior.";
            } else {
                // An alternative implementation option is to not report a finding at all in
                // this case, if we are worried about false positives causing confusion or
                // resulting in developers ignoring other lint warnings.
                report = "This broadcast receiver declares an intent-filter for a protected " +
                        "broadcast action string, which can only be sent by the system, " +
                        "not third-party applications. However, the receiver's onReceive " +
                        "method does not appear to call getAction to ensure that the " +
                        "received Intent's action string matches the expected value, " +
                        "potentially making it possible for another actor to send a " +
                        "spoofed intent with no action string or a different action " +
                        "string and cause undesired behavior. In this case, it is " +
                        "possible that the onReceive method passed the received Intent " +
                        "to another method that checked the action string. If so, this " +
                        "finding can safely be ignored.";
            }
            Location location = context.getNameLocation(method);
            context.report(ACTION_STRING, method, location, report);
        }
    }

    private static class OnReceiveVisitor extends AbstractUastVisitor {
        @NonNull private final JavaEvaluator mEvaluator;
        @Nullable private final PsiParameter mParameter;
        private boolean mCallsGetAction;
        private boolean mUsesIntent;

        public OnReceiveVisitor(@NonNull JavaEvaluator context, @Nullable PsiParameter parameter) {
            mEvaluator = context;
            mParameter = parameter;
        }

        public boolean getCallsGetAction() {
            return mCallsGetAction;
        }

        public boolean getUsesIntent() {
            return mUsesIntent;
        }

        @Override
        public boolean visitCallExpression(@NonNull UCallExpression node) {
            if (!mCallsGetAction && UastExpressionUtils.isMethodCall(node)) {
                PsiMethod method = node.resolve();
                if (method != null && "getAction".equals(method.getName()) &&
                        mEvaluator.isMemberInSubClassOf(method, CLASS_INTENT, false)) {
                    mCallsGetAction = true;
                }
            }

            return super.visitCallExpression(node);
        }

        @Override
        public boolean visitSimpleNameReferenceExpression(@NonNull USimpleNameReferenceExpression node) {
            if (!mUsesIntent && mParameter != null) {
                PsiElement resolved = node.resolve();
                if (mParameter.equals(resolved)) {
                    mUsesIntent = true;
                }
            }
            return super.visitSimpleNameReferenceExpression(node);
        }
    }
}
