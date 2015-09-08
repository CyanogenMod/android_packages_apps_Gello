LOCAL_PATH := $(call my-dir)

ifeq ($(WITH_GELLO_SOURCE),true)
bash gello_build.sh
include src/Android.mk
endif
