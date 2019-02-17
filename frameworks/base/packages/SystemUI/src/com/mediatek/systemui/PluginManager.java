package com.mediatek.systemui;

import android.content.Context;

import com.mediatek.common.MPlugin;
import com.mediatek.systemui.ext.DefaultMobileIconExt;
import com.mediatek.systemui.ext.IMobileIconExt;
import com.mediatek.systemui.ext.IStatusBarPlugin;
import com.mediatek.systemui.ext.DefaultStatusBarPlugin;

/**
 * A class to generate plugin object.
 */
public class PluginManager {
    private static IMobileIconExt sMobileIconExt = null;
    private static IStatusBarPlugin sSystemUIStatusBarExt = null;

    /**
     * Get MobileIcon plugin.
     * @param context Context
     * @return the plugin of IMobileIconExt.
     */
    public static synchronized IMobileIconExt getMobileIconExt(Context context) {
        if (sMobileIconExt == null) {
            sMobileIconExt = (IMobileIconExt) MPlugin.createInstance(
                    IMobileIconExt.class.getName(), context);
            if (sMobileIconExt == null) {
                sMobileIconExt = new DefaultMobileIconExt();
            }
        }
        return sMobileIconExt;
    }


    /**
     * Get a IStatusBarPlugin object with Context.
     *
     * @param context A Context object.
     * @return IStatusBarPlugin object.
     */
    public static synchronized IStatusBarPlugin getSystemUIStatusBarExt(Context context) {
        if (sSystemUIStatusBarExt == null) {
            sSystemUIStatusBarExt = new DefaultStatusBarPlugin(context);
        }

        IStatusBarPlugin statusBarExt =
            (IStatusBarPlugin) MPlugin.createInstance(
                IStatusBarPlugin.class.getName(), context);
        if (statusBarExt == null) {
            statusBarExt = sSystemUIStatusBarExt;
        }

        return statusBarExt;
    }

}
