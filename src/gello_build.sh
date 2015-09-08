#!/bin/bash
#
#  Copyright (C) 2015 The CyanogenMod Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  Integrated SWE Build System for Gello
#

TOP_GELLO=$(realpath .)
SRC_DIR=$TOP_GELLO/src
BUILD_DIR=$TOP_GELLO/build
GCLIENT_FILE=$TOP_GELLO/.gclient
DONE_FILE=$TOP_GELLO/.cm_done

GELLO_SRC=$TOP_GELLO/gello
CAF_BROWSER=$SRC_DIR/swe/browser-caf
BUILD_BROWSER=$SRC_DIR/swe/browser
COMPILED_APK=$SRC_DIR/out/Release/apks/SWE_AndroidBrowser.apk

# Uncomment this for fully automated builds
FORCE_GELLO_SOURCE=true

function build_gello() {
    # Compile gello from sources
    cd $SRC_DIR
    . build/android/envsetup.sh
    # Build with GCC
    GYP_DEFINES="$GYP_DEFINES clang=0 OS=android" gclient runhooks -v
    # Compile
    ninja -C out/Release swe_android_browser_apk
    local BUILDRETURN=$?
    if [ "$BUILDRETURN" == 0 ]; then
        if [ -f "$COMPILED_APK" ]; then
            READY_APK=$TOP_GELLO/Gello.apk &> /dev/null
            if [ -f "$READY_APK" ]; then
                rm -rf $READY_APK
            fi
            cp $COMPILED_APK $READY_APK
            echo "Done! Apk located to $READY_APK"
        else
            echo "Unable to find output (Excepted to be at $COMPILED_APK)"
            exit 1
        fi
    else
        echo "Build failed!!"
        exit 3
    fi
}

function sync_swe() {
    if [ -f "$DONE_FILE" ]; then
        # We've already synced at least once.
        # Remove Gello from swe system
        rm -rf $BUILD_BROWSER &>/dev/null
        # If found, move swe to its original path
        if [ -d "$CAF_BROWSER" ]; then
            mv $CAF_BROWSER $BUILD_BROWSER
        else
            echo "W: Unable to find CAF Browser, first build? (Excepted to be at $CAF_BROWSER)"
        fi
        # Start now
        echo "Syncing now!"
        gclient sync -n --no-nag-max
        local SYNCRETURN=$?
        if [ "$SYNCRETURN" == 0 ]; then
            if [ -d "$GELLO_SRC" ]; then
                # Move src/swe/browser to src/swe/browser-caf
                # and copy ../gello to src/swe/browser so it will be built
                mv $BUILD_BROWSER $CAF_BROWSER
                cp -r $GELLO_SRC $BUILD_BROWSER
            else
                echo "Aborting: Unable to find Gello sources! (Excepted to be at $GELLO_SRC)"
                exit 1
            fi
        else
            echo "Something went wrong while syncing!"
            exit 1
        fi
    else
        # First Sync
        echo "This will download a large amount of data. If you want a prebuilt, 'export WITH_GELLO_SOURCE=false'"
        gclient sync -n --no-nag-max
        local SYNCRETURN=$?
        if [ "$SYNCRETURN" == 0 ]; then
            # Everything was fine, let's move gello to its home
            if [ -d "$CAF_BROWSER" ]; then
                if [ -d "$GELLO_SRC" ]; then
                    # Move src/swe/browser to src/swe/browser-caf
                    # and copy ../gello to src/swe/browser so it will be built
                    mv $BUILD_BROWSER $CAF_BROWSER
                    cp -r $GELLO_SRC $BUILD_BROWSER
                    if [ -f $DONE_FILE ]; then
                        touch $DONE_FILE
                    fi
                else
                    echo "Unable to find Gello sources! (Excepted to be at $GELLO_SRC)"
                    exit 1
                fi
            else
                echo "W: Unable to find CAF Browser! (Excepted to be at $CAF_BROWSER)"
            fi
        else
            echo "Something went wrong while syncing!"
            exit 1
        fi
    fi
}

function check_system() {
    if [ -f "$GCLIENT_FILE" ]; then
        if [ ! -x $(which gclient) ]; then
            # Depot not installed, fail and return an url with setup info
            echo "Unable to find Depot tool! Make sure you have installed it, if not, see https://commondatastorage.googleapis.com/chrome-infra-docs/flat/depot_tools/docs/html/depot_tools_tutorial.html#_setting_up"
            exit 2
        fi
    else
        echo "Unable to find .gclient file, sync again! (Excepted to be at $GCLIENT_FILE)"
        exit 1
    fi
}

check_system && sync_swe && build_gello
