ifeq ($(GELLO_SRC),true)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := \
        android-common \
        guava \
        android-support-v13 \
        android-support-v4 \

ifeq ($(ENABLE_SWE_ENGINE),true)
#Build swe_res
SWE_PATH = $(TARGET_OUT_INTERMEDIATES)/APPS/Browser_intermediates/swe

SWE_BUILD := $(shell env -i ./packages/apps/Browser/tools/build_swe.sh $(SWE_PATH))

$(shell ln -s ../../../external/swe/src/out/Release/swe_test_apk/swe_res $(LOCAL_PATH)/.)
LOCAL_STATIC_JAVA_LIBRARIES += libsweengine
endif


LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) \
        src/com/android/browser/EventLogTags.logtags

LOCAL_PACKAGE_NAME := Browser

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_EMMA_COVERAGE_FILTER := *,-com.android.common.*

ifeq ($(ENABLE_SWE_ENGINE),true)
#symlink pak file from swe_res
$(shell ln -s ../swe_res/assets/webviewchromium.pak $(LOCAL_PATH)/assets/. -d)

#package swe so's to apk
prebuilt_libs := \
          swe_res/lib/

prebuilt_swe_libs := \
    $(foreach _file, $(wildcard $(LOCAL_PATH)/swe_res/lib/*.so),\
        $(notdir $(basename $(_file))))

prebuilt_swe_libs_full_path := \
   $(foreach _file, $(wildcard $(LOCAL_PATH)/swe_res/lib/*.so),\
        $(addprefix swe_res/lib/,$(notdir $(_file))))

LOCAL_REQUIRED_MODULES := $(prebuilt_swe_libs)

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, swe_res/content_res/res swe_res/ui_res/res swe_res/swe_res/res res)
LOCAL_AAPT_FLAGS := --auto-add-overlay --extra-packages org.chromium.content:org.chromium.ui:org.codeaurora.swe

$(echo $(SWE_BUILD))
endif

# We need the sound recorder for the Media Capture API.
LOCAL_REQUIRED_MODULES += SoundRecorder

include $(BUILD_PACKAGE)

ifeq ($(ENABLE_SWE_ENGINE),true)
#################################################
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libsweengine:swe_res/jar/swe_engine.jar
include $(BUILD_MULTI_PREBUILT)
################################################
endif

ifeq ($(ENABLE_SWE_ENGINE),true)
##############adding external .so to system/lib ##################
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_PREBUILT_LIBS := $(prebuilt_swe_libs_full_path)
include $(BUILD_MULTI_PREBUILT)
endif

# additionally, build tests in sub-folders in a separate .apk
include $(call all-makefiles-under,$(LOCAL_PATH))

endif
