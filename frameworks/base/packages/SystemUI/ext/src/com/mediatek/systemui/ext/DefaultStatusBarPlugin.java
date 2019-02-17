package com.mediatek.systemui.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.telephony.ServiceState;

import com.mediatek.systemui.statusbar.extcb.BehaviorSet;
import com.mediatek.systemui.statusbar.extcb.DataType;
import com.mediatek.systemui.statusbar.extcb.DefaultEmptySignalClusterExt;
import com.mediatek.systemui.statusbar.extcb.IconIdWrapper;
import com.mediatek.systemui.statusbar.extcb.NetworkType;
import com.mediatek.systemui.statusbar.extcb.SvLteController;

/**
 * Default implementation of Plug-in definition of Status bar.
 */
public class DefaultStatusBarPlugin extends ContextWrapper implements IStatusBarPlugin {

    /**
     * Constructs a new DefaultStatusBarPlugin instance with Context.
     * @param context A Context object
     */
    public DefaultStatusBarPlugin(Context context) {
        super(context);
    }

    @Override
    public void customizeSignalStrengthIcon(int level, boolean roaming, IconIdWrapper icon) {
    }

    @Override
    public void customizeSignalStrengthNullIcon(int slotId, IconIdWrapper icon) {
    }

    @Override
    public void customizeSignalStrengthOfflineIcon(int slotId, IconIdWrapper icon) {
    }

    @Override
    public void customizeSignalIndicatorIcon(int slotId, IconIdWrapper icon) {
    }

    @Override
    public void customizeDataTypeIcon(IconIdWrapper icon, boolean roaming,
            DataType dataType) {
    }

    @Override
    public void customizeDataNetworkTypeIcon(IconIdWrapper icon,
            boolean roaming, NetworkType networkType) {
    }

    @Override
    public void customizeDataNetworkTypeIcon(IconIdWrapper icon, boolean roaming,
            NetworkType networkType, SvLteController svLteController) {
    }

    @Override
    public void customizeDataActivityIcon(IconIdWrapper icon, int dataActivity) {
    }

    @Override
    public BehaviorSet customizeBehaviorSet() {
        return BehaviorSet.DEFAULT_BS;
    }

    @Override
    public boolean customizeHspaDistinguishable(boolean distinguishable) {
        return distinguishable;
    }

    @Override
    public boolean customizeHasNoSims(boolean orgHasNoSims) {
        return orgHasNoSims;
    }

    @Override
    public void customizeHDVoiceIcon(IconIdWrapper icon) {
    }

    @Override
    public ISignalClusterExt customizeSignalCluster() {
        return new DefaultEmptySignalClusterExt();
    }

    @Override
    public boolean updateSignalStrengthWifiOnlyMode(ServiceState serviceState,
                       boolean connected) {
        return connected;
    }
}
