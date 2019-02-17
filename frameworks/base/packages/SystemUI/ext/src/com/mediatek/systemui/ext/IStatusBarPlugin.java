package com.mediatek.systemui.ext;


import android.telephony.ServiceState;
import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.DataType;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.NetworkType;
import com.mediatek.systemui.statusbar.extcb.SvLteController;

/**
 * M: the interface for Plug-in definition of Status bar.
 */
public interface IStatusBarPlugin {

    /** Signal strength interfaces. @{ */

    /**
     * Customize signal strength icon.
     * @param level telephony signal strength leve.
     * @param roaming roaming Whether at roaming state.
     * @param icon The icon wrapper need to be customized.
     */
    void customizeSignalStrengthIcon(int level, boolean roaming, IconIdWrapper icon);

    /**
     * Customize Signal Strength icon when has no service or null.
     * @param slotId The slot index.
     * @param icon The icon wrapper need to be customized.
     */
    void customizeSignalStrengthNullIcon(int slotId, IconIdWrapper icon);

    /**
     * Customize Signal Strength icon when Off line.
     * @param slotId The slot index.
     * @param icon The icon wrapper need to be customized.
     */
    void customizeSignalStrengthOfflineIcon(int slotId, IconIdWrapper icon);

    /**
     * Customize the signal indicator icon.
     * @param slotId The slot index.
     * @param icon The icon wrapper need to be customized.
     */
    void customizeSignalIndicatorIcon(int slotId, IconIdWrapper icon);

    /** Signal strength interfaces. @} */

    /** Data connection interfaces. @{ */

    /**
     * Customize the data connection type icon.
     *
     * @param icon data connection type icon.
     * @param roaming roaming Whether at roaming state.
     * @param dataType The data connection type.
     */
    void customizeDataTypeIcon(IconIdWrapper icon, boolean roaming, DataType dataType);

    /**
     * Get the data connection network type icon.
     *
     * @param icon the need changed icon id wrapper.
     * @param roaming roaming Whether at roaming state.
     * @param networkType The network type.
     */
    void customizeDataNetworkTypeIcon(IconIdWrapper icon, boolean roaming,
            NetworkType networkType);

    /**
     * Get the data connection network type icon.
     *
     * @param icon the need changed icon id wrapper.
     * @param roaming roaming Whether at roaming state.
     * @param networkType The network type.
     * @param svLteController The SvLteController.
     */
    void customizeDataNetworkTypeIcon(IconIdWrapper icon, boolean roaming,
            NetworkType networkType, SvLteController svLteController);

    /**
     * @param icon the need changed icon id wrapper.
     * @param dataActivity the data activity index.
     */
    void customizeDataActivityIcon(IconIdWrapper icon, int dataActivity);

    /**
     *
     * @return Return the BehaviorSet for the different OP.
     */
    BehaviorSet customizeBehaviorSet();

    /** Resource interfaces. @{ */

    /** Resource interfaces. @} */

    /**
     * Customize the hspa distinguishable.
     *
     * @param distinguishable Default value.
     * @return The customized distinguishable value.
     */
    boolean customizeHspaDistinguishable(boolean distinguishable);

    /**
     * Customize Whether HasNoSims.
     *
     * @param orgHasNoSims default HasNoSims vaule.
     * @return true if HasNoSims.
     */
    boolean customizeHasNoSims(boolean orgHasNoSims);

    /**
     * Customize HDVoiceIcon.
     *
     * @param icon the HD Voice icon.
     */
    void customizeHDVoiceIcon(IconIdWrapper icon);

    /**
     * Customize SignalCluster view.
     * @return ISignalClusterExt
     */
    ISignalClusterExt customizeSignalCluster();

    /**
     * To remove network icons in case of wifi only mode for WFC.
     * @param serviceState the current service state.
     * @return whether in serive or not - false for iWLAN
     */
    boolean updateSignalStrengthWifiOnlyMode(ServiceState serviceState,
                boolean connected);
}
