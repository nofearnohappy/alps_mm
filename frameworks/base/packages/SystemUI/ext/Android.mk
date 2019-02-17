LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.mediatek.systemui.ext
LOCAL_JAVA_LIBRARIES += mediatek-framework

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_SRC_FILES += \
         ../src/com/mediatek/systemui/statusbar/util/SIMHelper.java \
         ../src/com/mediatek/systemui/statusbar/util/FeatureOptions.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/BehaviorSet.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/DataType.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/FeatureOptionUtils.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/IconIdWrapper.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/NetworkType.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/PhoneStateExt.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/DefaultEmptyNetworkControllerExt.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/DefaultEmptySignalClusterExt.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/INetworkControllerExt.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/ISignalClusterInfo.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/SvLteController.java \
         ../extcb/com/mediatek/systemui/statusbar/extcb/PluginFactory.java \


include $(BUILD_STATIC_JAVA_LIBRARY)
