package com.mediatek.systemui.statusbar.extcb;

import android.content.Context;
import android.util.Log;

import com.mediatek.common.MPlugin;
import com.mediatek.systemui.ext.DefaultQuickSettingsPlugin;
import com.mediatek.systemui.ext.DefaultStatusBarPlmnPlugin;
import com.mediatek.systemui.ext.DefaultStatusBarPlugin;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.IStatusBarPlmnPlugin;
import com.mediatek.systemui.ext.IStatusBarPlugin;

/**
 * M: Plug-in helper class as the facade for accessing related add-ons.
 */
public class PluginFactory {
    private static IStatusBarPlugin sStatusBarPlugin = null;
    private static boolean sIsDefaultStatusBarPlugin = true;
    private static IQuickSettingsPlugin sQuickSettingsPlugin = null;
    private static final String TAG = "PluginFactory";
    private static IStatusBarPlmnPlugin sStatusBarPlmnPlugin = null;

    /**
     * Get a IStatusBarPlugin object with Context.
     *
     * @param context A Context object.
     * @return IStatusBarPlugin object.
     */
    public static synchronized IStatusBarPlugin getStatusBarPlugin(Context context) {
        if (sStatusBarPlugin == null) {
            sStatusBarPlugin = (IStatusBarPlugin) MPlugin.createInstance(
                    IStatusBarPlugin.class.getName(), context);
            sIsDefaultStatusBarPlugin = false;
            if (sStatusBarPlugin == null) {
                sStatusBarPlugin = new DefaultStatusBarPlugin(context);
                sIsDefaultStatusBarPlugin = true;
            }
        }
        return sStatusBarPlugin;
    }

    /**
     * Get a IQuickSettingsPlugin object with Context.
     *
     * @param context A Context object.
     * @return IQuickSettingsPlugin object.
     */
    public static synchronized IQuickSettingsPlugin getQuickSettingsPlugin(Context context) {
        if (sQuickSettingsPlugin == null) {
            sQuickSettingsPlugin = (IQuickSettingsPlugin) MPlugin.createInstance(
                    IQuickSettingsPlugin.class.getName(), context);
            Log.d("@M_" + TAG, "getQuickSettingsPlugin mQuickSettingsPlugin= " +
                    sQuickSettingsPlugin);

            if (sQuickSettingsPlugin == null) {
                sQuickSettingsPlugin = new DefaultQuickSettingsPlugin(context);
                Log.d("@M_" + TAG, "getQuickSettingsPlugin get DefaultQuickSettingsPlugin = "
                        + sQuickSettingsPlugin);
            }
        }
        return sQuickSettingsPlugin;
    }

    public static synchronized boolean isDefaultStatusBarPlugin() {
        return sIsDefaultStatusBarPlugin;
    }

    /**
     * Get a IStatusBarPlmnPlugin object with Context.
     *
     * @param context A Context object.
     * @return IStatusBarPlmnPlugin object.
     */
    public static synchronized IStatusBarPlmnPlugin getStatusBarPlmnPlugin(Context context) {
        if (sStatusBarPlmnPlugin == null) {
            sStatusBarPlmnPlugin = (IStatusBarPlmnPlugin) MPlugin.createInstance(
                    IStatusBarPlmnPlugin.class.getName(), context);
            if (sStatusBarPlmnPlugin == null) {
                sStatusBarPlmnPlugin = new DefaultStatusBarPlmnPlugin(context);
            }
            Log.d("@M_" + TAG, "getStatusBarPlmnPlugin: " + sStatusBarPlmnPlugin);
        }
        return sStatusBarPlmnPlugin;
    }
}
