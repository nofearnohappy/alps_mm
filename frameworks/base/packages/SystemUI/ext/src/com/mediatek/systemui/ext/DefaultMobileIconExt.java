package com.mediatek.systemui.ext;

import com.mediatek.systemui.statusbar.util.FeatureOptions;


/**
 * Default implementation of Plug-in definition of IMobileIconExt.
 */
public class DefaultMobileIconExt implements IMobileIconExt {

    @Override
    public int customizeWifiNetCondition(int netCondition) {
        /// M: for CT 6M project, always set 1. @{
        if (FeatureOptions.MTK_CT6M_SUPPORT) {
            netCondition = 1;
        }
        /// @}
        return netCondition;
    }
    @Override
    public int customizeMobileNetCondition(int netCondition) {
        /// M: for CT 6M project, always set 1. @{
        if (FeatureOptions.MTK_CT6M_SUPPORT) {
            netCondition = 1;
        }
        /// @}
        return netCondition;
    }
}