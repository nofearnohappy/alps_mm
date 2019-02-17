/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.MathUtils;

import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.systemui.DemoMode;
import com.android.systemui.R;
import com.mediatek.systemui.statusbar.defaultaccount.DefaultAccountStatus;
import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.DataType;
import com.mediatek.systemui.statusbar.extcb.FeatureOptionUtils;
import com.mediatek.systemui.statusbar.extcb.INetworkControllerExt;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.NetworkType;
import com.mediatek.systemui.statusbar.extcb.PluginFactory;
import com.mediatek.systemui.statusbar.extcb.SvLteController;
import com.mediatek.systemui.statusbar.networktype.NetworkTypeUtils;
import com.mediatek.systemui.statusbar.util.SIMHelper;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Platform implementation of the network controller. **/
public class NetworkControllerImpl extends BroadcastReceiver
        implements NetworkController, DemoMode {
    // debug
    static final String TAG = "NetworkController";
    static final boolean DEBUG = true; //Log.isLoggable(TAG, Log.DEBUG);
    // additional diagnostics, but not logspew
    static final boolean CHATTY =  true; //Log.isLoggable(TAG + "Chat", Log.DEBUG);

    private static final int EMERGENCY_NO_CONTROLLERS = 0;
    private static final int EMERGENCY_FIRST_CONTROLLER = 100;
    private static final int EMERGENCY_VOICE_CONTROLLER = 200;
    private static final int EMERGENCY_NO_SUB = 300;

    private final Context mContext;
    private final TelephonyManager mPhone;
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;
    private final SubscriptionManager mSubscriptionManager;
    private final boolean mHasMobileDataFeature;
    private final SubscriptionDefaults mSubDefaults;
    private Config mConfig;

    // Subcontrollers.
    @VisibleForTesting
    final WifiSignalController mWifiSignalController;

    @VisibleForTesting
    final EthernetSignalController mEthernetSignalController;

    @VisibleForTesting
    final Map<Integer, MobileSignalController> mMobileSignalControllers =
            new HashMap<Integer, MobileSignalController>();
    // When no SIMs are around at setup, and one is added later, it seems to default to the first
    // SIM for most actions.  This may be null if there aren't any SIMs around.
    private MobileSignalController mDefaultSignalController;
    private final AccessPointControllerImpl mAccessPoints;
    private final MobileDataControllerImpl mMobileDataController;

    private boolean mInetCondition; // Used for Logging and demo.

    // BitSets indicating which network transport types (e.g., TRANSPORT_WIFI, TRANSPORT_MOBILE) are
    // connected and validated, respectively.
    private final BitSet mConnectedTransports = new BitSet();
    private final BitSet mValidatedTransports = new BitSet();

    // States that don't belong to a subcontroller.
    private boolean mAirplaneMode = false;
    private boolean mHasNoSims;
    private Locale mLocale = null;
    // This list holds our ordering.
    private List<SubscriptionInfo> mCurrentSubscriptions = new ArrayList<>();

    @VisibleForTesting
    boolean mListening;

    // The current user ID.
    private int mCurrentUserId;

    private OnSubscriptionsChangedListener mSubscriptionListener;

    // Handler that all broadcasts are received on.
    private final Handler mReceiverHandler;
    // Handler that all callbacks are made on.
    private final CallbackHandler mCallbackHandler;

    private int mEmergencySource;
    private boolean mIsEmergency;

    ///: M: Support for PLMN @{
    String[] mNetworkName;
    int mSlotCount = 0;
    /// @}

    @VisibleForTesting
    ServiceState mLastServiceState;

    /**
     * Construct this controller object and register for updates.
     */
    public NetworkControllerImpl(Context context, Looper bgLooper) {
        this(context, (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE),
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE),
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE),
                SubscriptionManager.from(context), Config.readConfig(context), bgLooper,
                new CallbackHandler(),
                new AccessPointControllerImpl(context, bgLooper),
                new MobileDataControllerImpl(context),
                new SubscriptionDefaults());
        mReceiverHandler.post(mRegisterListeners);
    }

    @VisibleForTesting
    NetworkControllerImpl(Context context, ConnectivityManager connectivityManager,
            TelephonyManager telephonyManager, WifiManager wifiManager,
            SubscriptionManager subManager, Config config, Looper bgLooper,
            CallbackHandler callbackHandler,
            AccessPointControllerImpl accessPointController,
            MobileDataControllerImpl mobileDataController,
            SubscriptionDefaults defaultsHandler) {
        mContext = context;
        mConfig = config;
        mReceiverHandler = new Handler(bgLooper);
        mCallbackHandler = callbackHandler;

        mSubscriptionManager = subManager;
        mSubDefaults = defaultsHandler;
        mConnectivityManager = connectivityManager;
        mHasMobileDataFeature =
                mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);

        /// M: Support "Operator plugin - Data activity/type, strength icon". @{
        mNetworkControllerExt = new NetworkControllerExt(mConfig);
        mSlotCount = SIMHelper.getSlotCount();
        mNetworkName = new String[mSlotCount];
        /// M: Support "Operator plugin - Data activity/type, strength icon". @}

        // telephony
        mPhone = telephonyManager;

        // wifi
        mWifiManager = wifiManager;

        mLocale = mContext.getResources().getConfiguration().locale;
        mAccessPoints = accessPointController;
        mMobileDataController = mobileDataController;
        mMobileDataController.setNetworkController(this);
        // TODO: Find a way to move this into MobileDataController.
        mMobileDataController.setCallback(new MobileDataControllerImpl.Callback() {
            @Override
            public void onMobileDataEnabled(boolean enabled) {
                mCallbackHandler.setMobileDataEnabled(enabled);
            }
        });
        mWifiSignalController = new WifiSignalController(mContext, mHasMobileDataFeature,
                mCallbackHandler, this);

        mEthernetSignalController = new EthernetSignalController(mContext, mCallbackHandler, this);

        // AIRPLANE_MODE_CHANGED is sent at boot; we've probably already missed it
        updateAirplaneMode(true /* force callback */);
    }

    private void registerListeners() {
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.registerListener();
        }
        if (mSubscriptionListener == null) {
            mSubscriptionListener = new SubListener();
        }
        mSubscriptionManager.addOnSubscriptionsChangedListener(mSubscriptionListener);

        // broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        filter.addAction(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(ConnectivityManager.INET_CONDITION_ACTION);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        addCustomizedAction(filter);
        mContext.registerReceiver(this, filter, null, mReceiverHandler);
        mListening = true;

        updateMobileControllers();
    }

    /// M: Add MTK more filter action
    private void addCustomizedAction(IntentFilter filter) {
        filter.addAction(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        /// Add for [VOLTE status icon]
        filter .addAction(ImsManager.ACTION_IMS_STATE_CHANGED);
        /// Receive preboot ipo broadcast.
        filter.addAction("android.intent.action.ACTION_PREBOOT_IPO");
    }
    /// @ }

    private void unregisterListeners() {
        mListening = false;
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.unregisterListener();
        }
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mSubscriptionListener);
        mContext.unregisterReceiver(this);
    }

    public int getConnectedWifiLevel() {
        return mWifiSignalController.getState().level;
    }

    @Override
    public AccessPointController getAccessPointController() {
        return mAccessPoints;
    }

    @Override
    public MobileDataController getMobileDataController() {
        return mMobileDataController;
    }

    public void addEmergencyListener(EmergencyListener listener) {
        mCallbackHandler.setListening(listener, true);
        mCallbackHandler.setEmergencyCallsOnly(isEmergencyOnly());
    }

    public boolean hasMobileDataFeature() {
        return mHasMobileDataFeature;
    }

    public boolean hasVoiceCallingFeature() {
        return mPhone.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE;
    }

    private MobileSignalController getDataController() {
        int dataSubId = mSubDefaults.getDefaultDataSubId();
        if (!SubscriptionManager.isValidSubscriptionId(dataSubId)) {
            if (DEBUG) Log.e(TAG, "No data sim selected");
            return mDefaultSignalController;
        }
        if (mMobileSignalControllers.containsKey(dataSubId)) {
            return mMobileSignalControllers.get(dataSubId);
        }
        if (DEBUG) Log.e(TAG, "Cannot find controller for data sub: " + dataSubId);
        return mDefaultSignalController;
    }

    public String getMobileDataNetworkName() {
        MobileSignalController controller = getDataController();
        return controller != null ? controller.getState().networkNameData : "";
    }

    public boolean isEmergencyOnly() {
        if (mMobileSignalControllers.size() == 0) {
            // When there are no active subscriptions, determine emengency state from last
            // broadcast.
            mEmergencySource = EMERGENCY_NO_CONTROLLERS;
            return mLastServiceState != null && mLastServiceState.isEmergencyOnly();
        }
        int voiceSubId = mSubDefaults.getDefaultVoiceSubId();
        if (!SubscriptionManager.isValidSubscriptionId(voiceSubId)) {
            for (MobileSignalController mobileSignalController :
                                            mMobileSignalControllers.values()) {
                if (!mobileSignalController.getState().isEmergency) {
                    mEmergencySource = EMERGENCY_FIRST_CONTROLLER
                            + mobileSignalController.mSubscriptionInfo.getSubscriptionId();
                    if (DEBUG) Log.d(TAG, "Found emergency " + mobileSignalController.mTag);
                    return false;
                }
            }
        }
        if (mMobileSignalControllers.containsKey(voiceSubId)) {
            mEmergencySource = EMERGENCY_VOICE_CONTROLLER + voiceSubId;
            if (DEBUG) Log.d(TAG, "Getting emergency from " + voiceSubId);
            return mMobileSignalControllers.get(voiceSubId).getState().isEmergency;
        }
        if (DEBUG) Log.e(TAG, "Cannot find controller for voice sub: " + voiceSubId);
        mEmergencySource = EMERGENCY_NO_SUB + voiceSubId;
        // Something is wrong, better assume we can't make calls...
        return true;
    }

    /**
     * Emergency status may have changed (triggered by MobileSignalController),
     * so we should recheck and send out the state to listeners.
     */
    void recalculateEmergency() {
        mIsEmergency = isEmergencyOnly();
        mCallbackHandler.setEmergencyCallsOnly(mIsEmergency);
    }

    public void addSignalCallback(SignalCallback cb) {
        mCallbackHandler.setListening(cb, true);
        mCallbackHandler.setSubs(mCurrentSubscriptions);
        mCallbackHandler.setIsAirplaneMode(new IconState(mAirplaneMode,
                TelephonyIcons.FLIGHT_MODE_ICON, R.string.accessibility_airplane_mode, mContext));
        mCallbackHandler.setNoSims(mHasNoSims);
        mWifiSignalController.notifyListeners();
        mEthernetSignalController.notifyListeners();
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.notifyListeners();
        }
    }

    @Override
    public void removeSignalCallback(SignalCallback cb) {
        mCallbackHandler.setListening(cb, false);
    }

    @Override
    public void setWifiEnabled(final boolean enabled) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                // Disable tethering if enabling Wifi
                final int wifiApState = mWifiManager.getWifiApState();
                if (enabled && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) ||
                        (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
                    mWifiManager.setWifiApEnabled(null, false);
                }

                mWifiManager.setWifiEnabled(enabled);
                return null;
            }
        }.execute();
    }

    @Override
    public void onUserSwitched(int newUserId) {
        mCurrentUserId = newUserId;
        mAccessPoints.onUserSwitched(newUserId);
        updateConnectivity();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (CHATTY) {
            Log.d(TAG, "onReceive: intent=" + intent);
        }
        final String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                action.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
            updateConnectivity();
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
            refreshLocale();
            boolean airplaneMode = intent.getBooleanExtra("state", false);
            updateAirplaneMode(airplaneMode, false);
        } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED)) {
            // We are using different subs now, we might be able to make calls.
            recalculateEmergency();
        } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
            // Notify every MobileSignalController so they can know whether they are the
            // data sim or not.
            for (MobileSignalController controller : mMobileSignalControllers.values()) {
                controller.handleBroadcast(intent);
            }
        } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
            // Might have different subscriptions now.
            updateMobileControllers();
            /// M: update plmn label @{
            refreshPlmnCarrierLabel();
            /// @}
        /// M: Support "subinfo record update". @{
        } else if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
            updateMobileControllersEx(intent);
            /// M: Support "subinfo record update". @}
        } else if (action.equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
            mLastServiceState = ServiceState.newFromBundle(intent.getExtras());
            if (mMobileSignalControllers.size() == 0) {
                // If none of the subscriptions are active, we might need to recalculate
                // emergency state.
                recalculateEmergency();
            }
        } else if (action.equals(ImsManager.ACTION_IMS_STATE_CHANGED)) {
            handleIMSAction(intent);
        }
        /// M: Fix ALPS02355838, when ipo boot, re-update the airplane mode status. @{
        else if (action.equals("android.intent.action.ACTION_PREBOOT_IPO")) {
            updateAirplaneMode(false);
        }
        /// @}
        else {
            int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                if (mMobileSignalControllers.containsKey(subId)) {
                    mMobileSignalControllers.get(subId).handleBroadcast(intent);
                } else {
                    // Can't find this subscription...  We must be out of date.
                    updateMobileControllers();
                }
            } else {
                // No sub id, must be for the wifi.
                mWifiSignalController.handleBroadcast(intent);
            }
        }
    }

    public void onConfigurationChanged() {
        mConfig = Config.readConfig(mContext);
        mReceiverHandler.post(new Runnable() {
            @Override
            public void run() {
                handleConfigurationChanged();
            }
        });
    }

    @VisibleForTesting
    void handleConfigurationChanged() {
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.setConfiguration(mConfig);
        }
        refreshLocale();
    }

    /// M: Support "subinfo record update".
    private void updateMobileControllersEx(Intent intent){
        int detectedType = SubscriptionManager.EXTRA_VALUE_NOCHANGE;
        if (intent != null) {
            detectedType = intent.getIntExtra(SubscriptionManager.INTENT_KEY_DETECT_STATUS, 0);
            Log.d(TAG, "updateMobileControllers detectedType: " + detectedType);
        }
        // If there have been no relevant changes to any of the subscriptions, we can leave as is.
        if (detectedType != SubscriptionManager.EXTRA_VALUE_REPOSITION_SIM) {
            // Even if the controllers are correct, make sure we have the right no sims state.
            // Such as on boot, don't need any controllers, because there are no sims,
            // but we still need to update the no sim state.
            updateNoSims();
            return;
        }

        updateMobileControllers();
    }

    private void updateMobileControllers() {
        /// M: SIMHelper update Active SubscriptionInfo
        SIMHelper.updateSIMInfos(mContext);

        if (!mListening) {
            if (DEBUG) {
                Log.d(TAG, "updateMobileControllers: it's not listening");
            }
            return;
        }
        doUpdateMobileControllers();
    }

    @VisibleForTesting
    void doUpdateMobileControllers() {
        List<SubscriptionInfo> subscriptions = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptions == null) {
            Log.d(TAG, "subscriptions is null");
            subscriptions = Collections.emptyList();
        }
        // If there have been no relevant changes to any of the subscriptions, we can leave as is.
        if (hasCorrectMobileControllers(subscriptions)) {
            // Even if the controllers are correct, make sure we have the right no sims state.
            // Such as on boot, don't need any controllers, because there are no sims,
            // but we still need to update the no sim state.
            updateNoSims();
            return;
        }
        setCurrentSubscriptions(subscriptions);
        updateNoSims();
        recalculateEmergency();
    }

    @VisibleForTesting
    protected void updateNoSims() {
        boolean hasNoSims = mHasMobileDataFeature && mMobileSignalControllers.size() == 0;
        if (hasNoSims != mHasNoSims) {
            mHasNoSims = hasNoSims;
            mCallbackHandler.setNoSims(mHasNoSims);
        }
    }

    @VisibleForTesting
    void setCurrentSubscriptions(List<SubscriptionInfo> subscriptions) {
        Collections.sort(subscriptions, new Comparator<SubscriptionInfo>() {
            @Override
            public int compare(SubscriptionInfo lhs, SubscriptionInfo rhs) {
                return lhs.getSimSlotIndex() == rhs.getSimSlotIndex()
                        ? lhs.getSubscriptionId() - rhs.getSubscriptionId()
                        : lhs.getSimSlotIndex() - rhs.getSimSlotIndex();
            }
        });
        mCurrentSubscriptions = subscriptions;

        HashMap<Integer, MobileSignalController> cachedControllers =
                new HashMap<Integer, MobileSignalController>(mMobileSignalControllers);
        mMobileSignalControllers.clear();
        final int num = subscriptions.size();
        for (int i = 0; i < num; i++) {
            int subId = subscriptions.get(i).getSubscriptionId();
            // If we have a copy of this controller already reuse it, otherwise make a new one.
            if (cachedControllers.containsKey(subId)) {
                /// M: Fix bug ALPS02416794 @{
                MobileSignalController msc = cachedControllers.remove(subId);
                msc.mSubscriptionInfo = subscriptions.get(i);
                mMobileSignalControllers.put(subId, msc);
                /// @}
            } else {
                MobileSignalController controller = new MobileSignalController(mContext, mConfig,
                        mHasMobileDataFeature, mPhone, mCallbackHandler,
                        this, subscriptions.get(i), mSubDefaults, mReceiverHandler.getLooper());
                mMobileSignalControllers.put(subId, controller);
                if (subscriptions.get(i).getSimSlotIndex() == 0) {
                    mDefaultSignalController = controller;
                }
                if (mListening) {
                    controller.registerListener();
                }
            }
        }
        if (mListening) {
            for (Integer key : cachedControllers.keySet()) {
                if (cachedControllers.get(key) == mDefaultSignalController) {
                    mDefaultSignalController = null;
                }
                cachedControllers.get(key).unregisterListener();
            }
        }
        mCallbackHandler.setSubs(subscriptions);
        notifyAllListeners();

        // There may be new MobileSignalControllers around, make sure they get the current
        // inet condition and airplane mode.
        pushConnectivityToSignals();
        updateAirplaneMode(true /* force */);
    }

    @VisibleForTesting
    boolean hasCorrectMobileControllers(List<SubscriptionInfo> allSubscriptions) {
        if (allSubscriptions.size() != mMobileSignalControllers.size()) {
            Log.d(TAG,"size not equals, reset subInfo");
            return false;
        }
        /// M: Fix bug ALPS02416794 for check subid that map order is whether match.@{
        for (SubscriptionInfo info : allSubscriptions) {
            /*if (!mMobileSignalControllers.containsKey(info.getSubscriptionId())) {
                return false;
            }*/
            MobileSignalController msc = mMobileSignalControllers.get(info.getSubscriptionId());
            if (msc == null || msc.mSubscriptionInfo.getSimSlotIndex() != info.getSimSlotIndex()) {
                Log.d(TAG, "info_subId = " + info.getSubscriptionId() +
                       " info_slotId = " + info.getSimSlotIndex());
                return false;
            }
        }
        /// @}
        return true;
    }

    private void updateAirplaneMode(boolean force) {
        boolean airplaneMode = (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1);
        /**
         *  M: Extract the method to another function called
         *  {@link updateAirplaneMode(boolean airplaneMode, boolean force)}
         */
        updateAirplaneMode(airplaneMode, force);
    }

    /// M: Since in broadcast case the provider may have delay, so extract a method
    /// and pass airplane mode state. @ {
    private void updateAirplaneMode(boolean airplaneMode, boolean force) {
        if (airplaneMode != mAirplaneMode || force) {
            mAirplaneMode = airplaneMode;
            for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
                mobileSignalController.setAirplaneMode(mAirplaneMode);
            }
            notifyListeners();
        }
    }
    /// @ }

    private void refreshLocale() {
        Locale current = mContext.getResources().getConfiguration().locale;
        if (!current.equals(mLocale)) {
            mLocale = current;
            notifyAllListeners();
        }
    }

    /**
     * Forces update of all callbacks on both SignalClusters and
     * NetworkSignalChangedCallbacks.
     */
    private void notifyAllListeners() {
        notifyListeners();
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.notifyListeners();
        }
        mWifiSignalController.notifyListeners();
        mEthernetSignalController.notifyListeners();
    }

    /**
     * Notifies listeners of changes in state of to the NetworkController, but
     * does not notify for any info on SignalControllers, for that call
     * notifyAllListeners.
     */
    private void notifyListeners() {
        mCallbackHandler.setIsAirplaneMode(new IconState(mAirplaneMode,
                TelephonyIcons.FLIGHT_MODE_ICON, R.string.accessibility_airplane_mode, mContext));
        mCallbackHandler.setNoSims(mHasNoSims);
    }

    /**
     * Update the Inet conditions and what network we are connected to.
     */
    private void updateConnectivity() {
        mConnectedTransports.clear();
        mValidatedTransports.clear();
        for (NetworkCapabilities nc :
                mConnectivityManager.getDefaultNetworkCapabilitiesForUser(mCurrentUserId)) {
            for (int transportType : nc.getTransportTypes()) {
                mConnectedTransports.set(transportType);
                if (nc.hasCapability(NET_CAPABILITY_VALIDATED)) {
                    mValidatedTransports.set(transportType);
                }
            }
        }

        if (CHATTY) {
            Log.d(TAG, "updateConnectivity: mConnectedTransports=" + mConnectedTransports);
            Log.d(TAG, "updateConnectivity: mValidatedTransports=" + mValidatedTransports);
        }

        mInetCondition = !mValidatedTransports.isEmpty();

        pushConnectivityToSignals();
    }

    /**
     * Pushes the current connectivity state to all SignalControllers.
     */
    private void pushConnectivityToSignals() {
        // We want to update all the icons, all at once, for any condition change
        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.updateConnectivity(mConnectedTransports, mValidatedTransports);
        }
        mWifiSignalController.updateConnectivity(mConnectedTransports, mValidatedTransports);
        mEthernetSignalController.updateConnectivity(mConnectedTransports, mValidatedTransports);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NetworkController state:");

        pw.println("  - telephony ------");
        pw.print("  hasVoiceCallingFeature()=");
        pw.println(hasVoiceCallingFeature());

        pw.println("  - connectivity ------");
        pw.print("  mConnectedTransports=");
        pw.println(mConnectedTransports);
        pw.print("  mValidatedTransports=");
        pw.println(mValidatedTransports);
        pw.print("  mInetCondition=");
        pw.println(mInetCondition);
        pw.print("  mAirplaneMode=");
        pw.println(mAirplaneMode);
        pw.print("  mLocale=");
        pw.println(mLocale);
        pw.print("  mLastServiceState=");
        pw.println(mLastServiceState);
        pw.print("  mIsEmergency=");
        pw.println(mIsEmergency);
        pw.print("  mEmergencySource=");
        pw.println(emergencyToString(mEmergencySource));

        for (MobileSignalController mobileSignalController : mMobileSignalControllers.values()) {
            mobileSignalController.dump(pw);
        }
        mWifiSignalController.dump(pw);

        mEthernetSignalController.dump(pw);

        mAccessPoints.dump(pw);
    }

    private static final String emergencyToString(int emergencySource) {
        if (emergencySource > EMERGENCY_NO_SUB) {
            return "NO_SUB(" + (emergencySource - EMERGENCY_NO_SUB) + ")";
        } else if (emergencySource > EMERGENCY_VOICE_CONTROLLER) {
            return "VOICE_CONTROLLER(" + (emergencySource - EMERGENCY_VOICE_CONTROLLER) + ")";
        } else if (emergencySource > EMERGENCY_FIRST_CONTROLLER) {
            return "FIRST_CONTROLLER(" + (emergencySource - EMERGENCY_FIRST_CONTROLLER) + ")";
        } else if (emergencySource == EMERGENCY_NO_CONTROLLERS) {
            return "NO_CONTROLLERS";
        }
        return "UNKNOWN_SOURCE";
    }

    private boolean mDemoMode;
    private boolean mDemoInetCondition;
    private WifiSignalController.WifiState mDemoWifiState;

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!mDemoMode && command.equals(COMMAND_ENTER)) {
            if (DEBUG) Log.d(TAG, "Entering demo mode");
            unregisterListeners();
            mDemoMode = true;
            mDemoInetCondition = mInetCondition;
            mDemoWifiState = mWifiSignalController.getState();
        } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
            if (DEBUG) Log.d(TAG, "Exiting demo mode");
            mDemoMode = false;
            // Update what MobileSignalControllers, because they may change
            // to set the number of sim slots.
            updateMobileControllers();
            for (MobileSignalController controller : mMobileSignalControllers.values()) {
                controller.resetLastState();
            }
            mWifiSignalController.resetLastState();
            mReceiverHandler.post(mRegisterListeners);
            notifyAllListeners();
        } else if (mDemoMode && command.equals(COMMAND_NETWORK)) {
            String airplane = args.getString("airplane");
            if (airplane != null) {
                boolean show = airplane.equals("show");
                mCallbackHandler.setIsAirplaneMode(new IconState(show,
                        TelephonyIcons.FLIGHT_MODE_ICON, R.string.accessibility_airplane_mode,
                        mContext));
            }
            String fully = args.getString("fully");
            if (fully != null) {
                mDemoInetCondition = Boolean.parseBoolean(fully);
                BitSet connected = new BitSet();

                if (mDemoInetCondition) {
                    connected.set(mWifiSignalController.mTransportType);
                }
                mWifiSignalController.updateConnectivity(connected, connected);
                for (MobileSignalController controller : mMobileSignalControllers.values()) {
                    if (mDemoInetCondition) {
                        connected.set(controller.mTransportType);
                    }
                    controller.updateConnectivity(connected, connected);
                }
            }
            String wifi = args.getString("wifi");
            if (wifi != null) {
                boolean show = wifi.equals("show");
                String level = args.getString("level");
                if (level != null) {
                    mDemoWifiState.level = level.equals("null") ? -1
                            : Math.min(Integer.parseInt(level), WifiIcons.WIFI_LEVEL_COUNT - 1);
                    mDemoWifiState.connected = mDemoWifiState.level >= 0;
                }
                mDemoWifiState.enabled = show;
                mWifiSignalController.notifyListeners();
            }
            String sims = args.getString("sims");
            if (sims != null) {
                int num = MathUtils.constrain(Integer.parseInt(sims), 1, 8);
                List<SubscriptionInfo> subs = new ArrayList<>();
                if (num != mMobileSignalControllers.size()) {
                    mMobileSignalControllers.clear();
                    int start = mSubscriptionManager.getActiveSubscriptionInfoCountMax();
                    for (int i = start /* get out of normal index range */; i < start + num; i++) {
                        subs.add(addSignalController(i, i));
                    }
                    mCallbackHandler.setSubs(subs);
                }
            }
            String nosim = args.getString("nosim");
            if (nosim != null) {
                mHasNoSims = nosim.equals("show");
                mCallbackHandler.setNoSims(mHasNoSims);
            }
            String mobile = args.getString("mobile");
            if (mobile != null) {
                boolean show = mobile.equals("show");
                String datatype = args.getString("datatype");
                String slotString = args.getString("slot");
                int slot = TextUtils.isEmpty(slotString) ? 0 : Integer.parseInt(slotString);
                slot = MathUtils.constrain(slot, 0, 8);
                // Ensure we have enough sim slots
                List<SubscriptionInfo> subs = new ArrayList<>();
                while (mMobileSignalControllers.size() <= slot) {
                    int nextSlot = mMobileSignalControllers.size();
                    subs.add(addSignalController(nextSlot, nextSlot));
                }
                if (!subs.isEmpty()) {
                    mCallbackHandler.setSubs(subs);
                }
                // Hack to index linearly for easy use.
                MobileSignalController controller = mMobileSignalControllers
                        .values().toArray(new MobileSignalController[0])[slot];
                controller.getState().dataSim = datatype != null;
                if (datatype != null) {
                    controller.getState().iconGroup =
                            datatype.equals("1x") ? TelephonyIcons.ONE_X :
                            datatype.equals("3g") ? TelephonyIcons.THREE_G :
                            datatype.equals("4g") ? TelephonyIcons.FOUR_G :
                            datatype.equals("e") ? TelephonyIcons.E :
                            datatype.equals("g") ? TelephonyIcons.G :
                            datatype.equals("h") ? TelephonyIcons.H :
                            datatype.equals("lte") ? TelephonyIcons.LTE :
                            datatype.equals("roam") ? TelephonyIcons.ROAMING :
                            TelephonyIcons.UNKNOWN;
                }
                int[][] icons = TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH;
                String level = args.getString("level");
                if (level != null) {
                    controller.getState().level = level.equals("null") ? -1
                            : Math.min(Integer.parseInt(level), icons[0].length - 1);
                    controller.getState().connected = controller.getState().level >= 0;
                }
                controller.getState().enabled = show;
                controller.notifyListeners();
            }
            String carrierNetworkChange = args.getString("carriernetworkchange");
            if (carrierNetworkChange != null) {
                boolean show = carrierNetworkChange.equals("show");
                for (MobileSignalController controller : mMobileSignalControllers.values()) {
                    controller.setCarrierNetworkChangeMode(show);
                }
            }
        }
    }

    private SubscriptionInfo addSignalController(int id, int simSlotIndex) {
        SubscriptionInfo info = new SubscriptionInfo(id, "", simSlotIndex, "", "", 0, 0, "", 0,
                null, 0, 0, "");
        mMobileSignalControllers.put(id, new MobileSignalController(mContext,
                mConfig, mHasMobileDataFeature, mPhone, mCallbackHandler, this, info,
                mSubDefaults, mReceiverHandler.getLooper()));
        return info;
    }

    private class SubListener extends OnSubscriptionsChangedListener {
        @Override
        public void onSubscriptionsChanged() {
            updateMobileControllers();
        }
    }
    /**
     * Used to register listeners from the BG Looper, this way the PhoneStateListeners that
     * get created will also run on the BG Looper.
     */
    private final Runnable mRegisterListeners = new Runnable() {
        @Override
        public void run() {
            registerListeners();
        }
    };

    public interface EmergencyListener {
        void setEmergencyCallsOnly(boolean emergencyOnly);
    }

    public static class SubscriptionDefaults {
        public int getDefaultVoiceSubId() {
            return SubscriptionManager.getDefaultVoiceSubId();
        }

        public int getDefaultDataSubId() {
            return SubscriptionManager.getDefaultDataSubId();
        }
    }
    /// M: Support[Network Type on Statusbar]
    /// public the class so other class able to access config.
    @VisibleForTesting
    public static class Config {
        public boolean showAtLeast3G = false;
        public boolean alwaysShowCdmaRssi = false;
        public boolean show4gForLte = false;
        public boolean hspaDataDistinguishable;

        static Config readConfig(Context context) {
            Config config = new Config();
            Resources res = context.getResources();

            config.showAtLeast3G = res.getBoolean(R.bool.config_showMin3G);
            config.alwaysShowCdmaRssi =
                    res.getBoolean(com.android.internal.R.bool.config_alwaysUseCdmaRssi);
            config.show4gForLte = res.getBoolean(R.bool.config_show4GForLTE);
            config.hspaDataDistinguishable =
                    res.getBoolean(R.bool.config_hspa_data_distinguishable);
            return config;
        }
    }

    /// M: Support [SIM Indicator] @ {
    public void showDefaultAccountStatus(DefaultAccountStatus status) {
        mCallbackHandler.setDefaultAccountStatus(status);
    }
    /// M: Support [Volte icon status]
    private void handleIMSAction(Intent intent) {
        int regState = intent.getIntExtra(ImsManager.EXTRA_IMS_REG_STATE_KEY,
                ServiceState.STATE_OUT_OF_SERVICE);
        int phoneId = intent.getIntExtra(ImsManager.EXTRA_PHONE_ID,
                SubscriptionManager.INVALID_PHONE_INDEX);
        int imsSubId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        Log.d(TAG,"handleIMSAction regState = " + regState + " phoneId = " + phoneId);
        int iconId = 0;
        for (Integer subId : mMobileSignalControllers.keySet()) {
            MobileSignalController signalController = mMobileSignalControllers.get(subId);
            if (subId == imsSubId) {
                signalController.setImsRegState(regState);
                iconId = regState == ServiceState.STATE_IN_SERVICE &&
                                     signalController.isLteNetWork() ?
                                     getVolteIconId(phoneId) : 0;
                Log.d(TAG,"Set IMS regState with iconId = " + iconId);
            } else {
                //Reset all state for other mobilesignalcontroller
                Log.d(TAG,"Reset ims register state for other sim");
                signalController.setImsRegState(-1);
            }
        }
        // In case hot plug, so always to refresh icon state
        mCallbackHandler.setVolteStatusIcon(iconId);
    }

    public int getVolteIconId(int slotId) {
        final TelephonyManager tm =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        int slotCount = tm.getSimCount();
        int iconId = R.drawable.stat_sys_volte;
        if (slotCount > 1 && slotId < NetworkTypeUtils.VOLTEICON.length) {
            iconId = NetworkTypeUtils.VOLTEICON[slotId];
        }
        return iconId;
    }

    /// @ }

    /// M: Support "Operator plugin - Customize Carrier Label for PLMN". @{
    /**
     * Recalculate and update the plmn carrier label.
     */
    public void refreshPlmnCarrierLabel() {
        for (int i = 0; i < mSlotCount; i++) {
            boolean found = false;
            for (Map.Entry<Integer, MobileSignalController> entry : mMobileSignalControllers
                    .entrySet()) {
                int subId = entry.getKey();
                int slotId = -1;
                MobileSignalController controller = entry.getValue();

                if (controller.getControllerSubInfo() != null) {
                    slotId = controller.getControllerSubInfo().getSimSlotIndex();
                }

                if (i == slotId) {
                    mNetworkName[slotId] = controller.mCurrentState.networkName;
                    PluginFactory.getStatusBarPlmnPlugin(mContext).updateCarrierLabel(i, true,
                            controller.getControllserHasService(), mNetworkName);
                    found = true;
                    break;
                }
            }
            // not have sub
            if (!found) {
                mNetworkName[i] = mContext
                        .getString(com.android.internal.R.string.lockscreen_carrier_default);
                PluginFactory.getStatusBarPlmnPlugin(mContext).updateCarrierLabel(i, false, false,
                        mNetworkName);
            }
        }
    }
    /// M: Support "Operator plugin - Customize Carrier Label for PLMN". @}

    /// M: Support "Operator plugin - Data activity/type, strength icon". @{
    private final NetworkControllerExt mNetworkControllerExt;

    private final MobileSignalController getMobileSignalController(int subId) {
        if (mMobileSignalControllers.containsKey(subId)) {
            return mMobileSignalControllers.get(subId);
        }
        Log.e(TAG, "Cannot find controller for sub: " + subId);
        return null;
    }

    /**
     * Get a INetworkControllerExt instance.
     *
     * @return INetworkControllerExt.
     */
    public final NetworkControllerExt getNetworkControllerExt() {
        return mNetworkControllerExt;
    }

    /**
     * M: Operator INetworkControllerExt implements.
     */
    private class NetworkControllerExt implements INetworkControllerExt {
        private static final String TAG = "StatusBar.NetworkControllerExt";
        private boolean mDebug = !FeatureOptionUtils.isUserLoad();

        private final Config mConfig;

        public NetworkControllerExt(final Config config) {
            this.mConfig = config;
            config.hspaDataDistinguishable = PluginFactory.getStatusBarPlugin(mContext)
                    .customizeHspaDistinguishable(config.hspaDataDistinguishable);

            DataType.mapDataTypeSets(config.showAtLeast3G, config.show4gForLte,
                    config.hspaDataDistinguishable);
            NetworkType.mapNetworkTypeSets(config.showAtLeast3G, config.show4gForLte,
                    config.hspaDataDistinguishable);
        }

        @Override
        public boolean isShowAtLeast3G() {
            return mConfig.showAtLeast3G;
        }

        @Override
        public boolean isHspaDataDistinguishable() {
            return mConfig.hspaDataDistinguishable;
        }

        @Override
        public boolean hasMobileDataFeature() {
            return mHasMobileDataFeature;
        }

        @Override
        public Resources getResources() {
            return mContext.getResources();
        }

        @Override
        public void getDefaultSignalNullIcon(IconIdWrapper icon) {
            icon.setResources(mContext.getResources());
            icon.setIconId(TelephonyIcons.TELEPHONY_NO_NETWORK);
        }

        @Override
        public void getDefaultRoamingIcon(IconIdWrapper icon) {
            icon.setResources(mContext.getResources());
            icon.setIconId(TelephonyIcons.ROAMING_ICON);
        }

        @Override
        public boolean hasService(int subId) {
            final MobileSignalController controller = getMobileSignalController(subId);
            final boolean hasService = controller != null ?
                    controller.getControllserHasService() : false;
            return hasService;
        }

        @Override
        public boolean isDataConnected(int subId) {
            final MobileSignalController controller = getMobileSignalController(subId);
            return controller != null ? controller.getState().dataConnected : false;
        }

        @Override
        public boolean isRoaming(int subId) {
            final MobileSignalController controller = getMobileSignalController(subId);
            final boolean isRoaming = controller != null ?
                    controller.getControllserIsRoaming() : false;
            return isRoaming;
        }

        @Override
        public int getSignalStrengthLevel(int subId) {
            final MobileSignalController controller = getMobileSignalController(subId);
            final int level = controller != null ? controller.mCurrentState.level
                    : SignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
            return level;
        }

        @Override
        public NetworkType getNetworkType(int subId) {
            final int dataNetType = getDataNetworkType(subId);
            return NetworkType.get(dataNetType);
        }

        @Override
        public DataType getDataType(int subId) {
            final MobileSignalController controller = getMobileSignalController(subId);
            final int dataNetType = controller != null ? controller.getControllserDataNetType()
                    : TelephonyManager.NETWORK_TYPE_UNKNOWN;
            return DataType.get(dataNetType);
        }

        @Override
        public int getDataActivity(int subId) {
            int activity = TelephonyManager.DATA_ACTIVITY_NONE;
            final MobileSignalController controller = getMobileSignalController(subId);
            if (controller != null && controller.mCurrentState.dataConnected) {
                if (controller.mCurrentState.activityIn
                        && controller.mCurrentState.activityOut) {
                    activity = TelephonyManager.DATA_ACTIVITY_INOUT;
                } else if (controller.mCurrentState.activityIn) {
                    activity = TelephonyManager.DATA_ACTIVITY_OUT;
                } else if (controller.mCurrentState.activityOut) {
                    activity = TelephonyManager.DATA_ACTIVITY_OUT;
                }
            }
            return activity;
        }

        @Override
        public boolean isLteTddSingleDataMode(int subId) {
            return false;
        }

        @Override
        public SvLteController getSvLteController(int subId) {
            final MobileSignalController controller = getMobileSignalController(subId);
            return controller != null ? controller.mSvLteController : null;
        }

        @Override
        public boolean isEmergencyOnly(int subId) {
            final MobileSignalController controller = getMobileSignalController(subId);
            boolean isOnlyEmergency = controller != null ? controller.isEmergencyOnly() : false;
            return isOnlyEmergency;
        }

        @Override
        public boolean isOffline(int subId) {
            boolean isOffline = false;
            final MobileSignalController controller = getMobileSignalController(subId);
            if (controller != null) {
                if (SvLteController.isMediatekSVLteDcSupport(controller.getControllerSubInfo()) ||
                        SvLteController.isMediatekSRLteDcSupport(
                                controller.getControllerSubInfo())) {
                    isOffline = controller.mSvLteController.isOffline(getNetworkType(subId));
                } else {
                    isOffline = controller.isEmergencyOnly();
                }
            }
            return isOffline;
        }

        @Override
        public boolean isRoamingGGMode() {
            boolean isRoamingGGMode = false;
            if (isBehaviorSet(BehaviorSet.OP09_BS)) {
                final SubscriptionInfo info = SIMHelper.getSubInfoBySlot(mContext,
                        PhoneConstants.SIM_ID_1);
                if (info != null) {
                    final MobileSignalController controller = getMobileSignalController(info
                            .getSubscriptionId());
                    isRoamingGGMode = controller != null ?
                            !controller.getControllserIsCdma() : false;
                }
            }
            if (mDebug) {
                Log.d(TAG, "isRoamingGGMode, slotId = " + PhoneConstants.SIM_ID_1
                        + ", isRoamingGGMode = " + isRoamingGGMode);
            }
            return isRoamingGGMode;
        }

        private final int getDataNetworkType(int subId) {
            // Big - Data
            int dataNetType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
            final MobileSignalController controller = getMobileSignalController(subId);
            if (controller != null) {
                if (mDebug) {
                    Log.d(TAG, "getDataNetworkType()"
                            + ", DataState = " + controller.getControllserDataState()
                            + ", ServiceState = " + controller.getControllserServiceState());
                }

                int networkTypeData = controller.getControllserDataNetType();
                ServiceState serviceState = controller.getControllserServiceState();
                if (serviceState != null) {
                    if (mDebug) {
                        Log.d(TAG, "getDataNetworkType: DataNetType = "
                                + controller.getControllserDataNetType() + " / "
                                + serviceState.getDataNetworkType());
                    }

                    if (controller.getControllserDataState() == TelephonyManager.DATA_UNKNOWN
                            || controller.getControllserDataState()
                                == TelephonyManager.DATA_DISCONNECTED) {
                        networkTypeData = serviceState.getDataNetworkType();
                    }

                    final int cs = serviceState.getVoiceNetworkType();
                    final int ps = networkTypeData;
                    if (mDebug) {
                        Log.d(TAG, "getNWTypeByPriority(), CS = " + cs + ", PS = " + ps);
                    }
                    dataNetType = getNWTypeByPriority(cs, ps);
                } else {
                    dataNetType = networkTypeData;
                }

                // Support SVLTE.
                if (SvLteController.isMediatekSVLteDcSupport(controller.getControllerSubInfo())) {
                    dataNetType = controller.mSvLteController
                            .getDataNetTypeWithLTEService(dataNetType);
                }

                Log.d(TAG, "getDataNetworkType: DataNetType="
                        + controller.getControllserDataNetType() + " / " + dataNetType);
            }

            return dataNetType;
        }

        private final int getNWTypeByPriority(int cs, int ps) {
            if (TelephonyManager.getNetworkClass(cs) > TelephonyManager.getNetworkClass(ps)) {
                return cs;
            } else {
                return ps;
            }
        }

        private final boolean isBehaviorSet(BehaviorSet behaviorSet) {
            return PluginFactory.getStatusBarPlugin(mContext).customizeBehaviorSet() == behaviorSet;
        }
    }
    /// M: Support "Operator plugin - Data activity/type, strength icon". @}
}
