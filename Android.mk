LOCAL_PATH := $(call my-dir)

# Gello_SRC is for build from sources, Gello is for prebuilt
LOCAL_MODULE := Gello_SRC

ifeq ($(WITH_GELLO_SOURCE),true)
bash gello_build.sh
include src/Android.mk
endif
