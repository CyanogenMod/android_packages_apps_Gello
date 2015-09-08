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

TOP_GELLO=$(realpath .)/src
SRC_DIR=$TOP_GELLO/src
DONE_FILE=$TOP_GELLO/../.cm_done

function gimme_my_gello() {
    local COMPILED_APK=$SRC_DIR/out/Release/apks/SWE_AndroidBrowser.apk
    local READY_APK=$TOP_GELLO/Gello.apk

    cd $SRC_DIR
    . build/android/envsetup.sh

    # Build with CyanogenMod channel
    GYP_DEFINES="$GYP_DEFINES OS=android swe_channel=cm" gclient runhooks
    ninja -C out/Release swe_android_browser_apk

    if [ "$?" == 0 ]; then
        if [ -f "$COMPILED_APK" ]; then
            # If found, remove previous compiled apk
            if [ -f "$READY_APK" ]; then
                rm -f $READY_APK
            fi
            # Copy new apk to its new home.
            cp $COMPILED_APK $READY_APK
            # A green message to comunicate it's done
            echo "$(tput setaf 2)Done! Apk located to $READY_APK$(tput sgr 0)"
        else
            echo "Unable to find output (Excepted to be at $COMPILED_APK)"
            exit 1
        fi
    else
        echo "Build failed!!"
        exit 3
    fi
}

function get_ready() {
    local GELLO_SRC=$TOP_GELLO/gello
    local CAF_BROWSER=$SRC_DIR/swe/browser-caf
    local BUILD_BROWSER=$SRC_DIR/swe/browser

    cd $SRC_DIR
    if [ -f "$DONE_FILE" ]; then
        # We've already synced at least once.
        # If needed remove Gello from swe system to avoid conflicts
        if [ -d $BUILD_BROWSER ]; then
            rm -rf $BUILD_BROWSER &>/dev/null
        fi
        # Move swe to its original path
        if [ -d "$CAF_BROWSER" ]; then
            mv $CAF_BROWSER $BUILD_BROWSER
        fi
    fi

    # Start now
    echo "Syncing now!"
    gclient sync -n -v --no-nag-max

    if [ "$?" == 0 ]; then
        # Move src/swe/browser to src/swe/browser-caf as backup/timesaver for future syncs
        mv $BUILD_BROWSER $CAF_BROWSER
        # We can't link (build will fail), so we'll just copy gello
        cp -r $GELLO_SRC $BUILD_BROWSER
        if [ ! -f $DONE_FILE ]; then
            touch $DONE_FILE
        fi
    else
        echo "Something went wrong while syncing!"
        exit 1
    fi
}

function make_clean() {
    # Remove everything and sync up again
    if [ -d "$TOP_GELLO/src" ];
        rm -rf $TOP_GELLO/src &> /dev/null
    fi
    if [ -d "$TOP_GELLO/build" ]; then
        rm -rf $TOP_GELLO/build &> /dev/null
    fi
    if [ -f "$TOP_GELLO/.gclient_entries" ]; then
        rm -f $TOP_GELLO/.gclient_entries
    fi
    if [ -d "$TOP_GELLO/_bad_scm" ]; then
        rm -rf $TOP_GELLO/_bad_scm &> /dev/null
    fi
}

##
# Main
#

if [ "$1" == "--force-clean" ]; then
    make_clean
    if [ "$?" == 0 ]; then
        echo "Build envirnoment cleaned!"
    else
        echo "Something went wrong while removing build envirnoment"
    fi
fi

# Check if running for the first time
if [ ! -f "$DONE_FILE" ]; then
    if [ ! -x $(which gclient) ]; then
        cd $TOP_GELLO/../tools
        echo "Depot tool not found! Installing..."
        git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
        cd $TOP_GELLO/..
    fi
    echo "First time for Gello!\nThis will download a large amount of data!! It can take a LOT of time.\nIf you want a prebuilt stop this and 'export WITH_GELLO_SOURCE=false'"
fi

# Sync
get_ready

# Create
gimme_my_gello
