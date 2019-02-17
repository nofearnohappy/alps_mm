package com.mediatek.systemui.statusbar.extcb;

/**
 * SignalCluster common Info to support ISignalClusterExt.
 */
public interface ISignalClusterInfo {
    /**
     * Whether wifi indicators is visible.
     *
     * @return true If wifi indicators is visible.
     */
    boolean isWifiIndicatorsVisible();

    /**
     * Whether show No Sims info.
     *
     * @return true If show No Sims info.
     */
    boolean isNoSimsVisible();

    /**
     * Whether Airplane Mode.
     *
     * @return true If is Airplane Mode.
     */
    boolean isAirplaneMode();

    /**
     * Get widetype icon start padding value.
     *
     * @return WideType icon start padding value.
     */
    int getWideTypeIconStartPadding();

    /**
     * Get secondary telephony padding value.
     *
     * @return Secondary telephony padding value.
     */
    int getSecondaryTelephonyPadding();
}
