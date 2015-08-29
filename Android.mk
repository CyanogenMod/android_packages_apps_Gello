LOCAL_PATH := $(call my-dir)

ifneq ($(WITH_GELLO_PREBUILT),true)
include $(LOCAL_PATH)/src/Android.mk
else
include $(LOCAL_PATH)/prebuilt/Android.mk
endif
