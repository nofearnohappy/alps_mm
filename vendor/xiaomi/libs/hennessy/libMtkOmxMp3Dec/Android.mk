LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE =
LOCAL_MODULE = libMtkOmxMp3Dec
LOCAL_MODULE_CLASS = SHARED_LIBRARIES
LOCAL_MODULE_PATH =
LOCAL_MODULE_RELATIVE_PATH =
LOCAL_MODULE_SUFFIX = .so
LOCAL_SHARED_LIBRARIES_32 = libbinder libui libmtk_drvb libc++
LOCAL_MULTILIB = 32
LOCAL_SRC_FILES_32 = arm/libMtkOmxMp3Dec.so
include $(BUILD_PREBUILT)
