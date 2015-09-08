LOCAL_PATH := $(call my-dir)

ifneq ($(WITH_GELLO_SOURCE),true)
include $(LOCAL_PATH)/prebuilt/src.mk
else
include $(LOCAL_PATH)/src/Android.mk
endif
