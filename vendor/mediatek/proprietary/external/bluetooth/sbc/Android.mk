LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libsbccodec
LOCAL_SRC_FILES := libsbccodec.so
LOCAL_MULTILIB := 32
#LOCAL_SHARED_LIBRARIES := 
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_SUFFIX := .so
include $(BUILD_PREBUILT)
