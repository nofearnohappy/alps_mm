package com.mediatek.systemui.ext;

import android.annotation.SuppressLint;
import android.telephony.SubscriptionInfo;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.mediatek.systemui.statusbar.extcb.INetworkControllerExt;
import com.mediatek.systemui.statusbar.extcb.ISignalClusterInfo;
import com.mediatek.systemui.statusbar.extcb.PhoneStateExt;

import java.util.List;

/**
 * M: SignalCluster ext interface to support "Operator plugin - Data activity/type, strength icon".
 */
public interface ISignalClusterExt {
    /**
     * Set SignalClusterInfo interface object.
     *
     * @param signalClusterInfo SignalClusterInfo interface object.
     */
    void setSignalClusterInfo(ISignalClusterInfo signalClusterInfo);

    /**
     * Set INetworkControllerExt.
     *
     * @param networkControllerExt INetworkControllerExt.
     */
    void setNetworkControllerExt(INetworkControllerExt networkControllerExt);

    /**
     * Set SubscriptionInfos.
     *
     * @param subs SubscriptionInfos.
     * @param states PhoneStateExts.
     */
    void setSubs(List<SubscriptionInfo> subs, PhoneStateExt[] states);

    /**
     * Set HDVoiceIcon.
     *
     * @param hDVoiceIcon HDVoice imageView.
     */
    void setHDVoiceIcon(ImageView hDVoiceIcon);

    /**
     * Set Mobile Data Indicators.
     *
     * @param subId the subId.
     * @param mobileVisible visibility.
     * @param signalClusterCombo signal cluster combo root ViewGroup.
     * @param mobileNetworkType Mobile Network Type ImageView.
     * @param mobileGroup Mobile root ViewGroup.
     * @param mobileStrength Mobile signal strength ImageView.
     * @param mobileType Mobile Type ImageView.
     * @param mobileStrengthIconId mobile signal strength icon id.
     * @param mobileDataTypeIconId mobile data type icon id.
     * @param mobileDescription mobile signal strength content description.
     * @param mobileTypeDescription mobile data type content description.
     * @param isMobileTypeIconWide whether mobile type icon is wide.
     */
    void setMobileDataIndicators(int subId, boolean mobileVisible,
            ViewGroup signalClusterCombo, ImageView mobileNetworkType,
            ViewGroup mobileGroup, ImageView mobileStrength, ImageView mobileType,
            int mobileStrengthIconId, int mobileDataTypeIconId,
            String mobileDescription, String mobileTypeDescription,
            boolean isMobileTypeIconWide);

    /**
     * This is called when the SignalClusterExt view is attached to a window.
     *
     * @param mobileSignalGroup signal cluster combos.
     * @param noSimsView No Sims ImageView.
     */
    void onAttachedToWindow(LinearLayout mobileSignalGroup, ImageView noSimsView);

    /**
     * This is called when the SignalClusterExt view is detached from a window.
     */
    @SuppressLint("MissingSuperCall")
    void onDetachedFromWindow();

    /**
     * Called when any RTL property (layout direction or text direction or text
     * alignment) has been changed.
     *
     * @param layoutDirection the direction of the layout
     */
    void onRtlPropertiesChanged(int layoutDirection);

    /**
     * Apply data to view, run after indicator change.
     */
    void apply();
}
