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
VERBOSE=false

##
# Sync and envsetup
#
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
    if [ "$1" != "--no-sync" ] && [ "$2" != "--no-sync" ]; then
        # use -v flag if it's requested or if it's the first time we sync
        if [ "$VERBOSE" == true ] || [ ! -f "$DONE_FILE" ]; then
            gclient sync -n -v --no-nag-max
            if [ "$?" != 0 ]; then
                echo "Something went wrong while syncing!"
                exit 1
            fi
        else
            echo "Syncing now!"
            gclient sync -n --no-nag-max
            local SYNCRETURN=$?
            if [ "$?" != 0 ]; then
                echo "Something went wrong while syncing!"
                exit 1
            fi
        fi
    fi

    # Move src/swe/browser to src/swe/browser-caf as backup/timesaver for future syncs
    # We can't link (build will fail), so we'll just copy gello to src/swe/browser
    mv $BUILD_BROWSER $CAF_BROWSER
    cp -r $GELLO_SRC $BUILD_BROWSER
    if [ ! -f $DONE_FILE ]; then
        touch $DONE_FILE
    fi

    . build/android/envsetup.sh
    echo "Updating build environment..."
    # Build with CyanogenMod channel
    if [ "$VERBOSE" == true ]; then
        GYP_DEFINES="$GYP_DEFINES OS=android swe_channel=cm" gclient runhooks
        local SETUPRETURN=$?
    else
        GYP_DEFINES="$GYP_DEFINES OS=android swe_channel=cm" gclient runhooks &>/dev/null
        local SETUPRETURN=$?
    fi
    if [ "$SETUPRETURN" == 0 ]; then
        echo "Build environment is ready!"
    else
        echo "Build environment setup failed."
        exit 3
    fi
}

##
# Gello compilation
#
function gimme_my_gello() {
    local COMPILED_APK=$SRC_DIR/out/Release/apks/SWE_AndroidBrowser.apk
    local READY_APK=$TOP_GELLO/Gello.apk

    cd $SRC_DIR
    echo "Compiling apk with ninja..."
    if [ "$VERBOSE" == true ]; then
        ninja -C out/Release swe_android_browser_apk
    else
        ninja -C out/Release swe_android_browser_apk &>/dev/null
    fi

    if [ "$?" == 0 ]; then
        if [ -f "$COMPILED_APK" ]; then
            # If found, remove previous compiled apk
            if [ -f "$READY_APK" ]; then
                rm -f $READY_APK
            fi
            # Copy new apk to its new home.
            cp $COMPILED_APK $READY_APK
            # A green message to comunicate we did it
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

##
# Cleaner
#
function make_clean() {
    # Remove everything and sync up again
    if [ -d "$TOP_GELLO/src" ]; then
        rm -rf $TOP_GELLO/src &>/dev/null
    fi
    if [ -d "$TOP_GELLO/build" ]; then
        rm -rf $TOP_GELLO/build &>/dev/null
    fi
    if [ -f "$TOP_GELLO/.gclient_entries" ]; then
        rm -f $TOP_GELLO/.gclient_entries
    fi
    if [ -d "$TOP_GELLO/_bad_scm" ]; then
        rm -rf $TOP_GELLO/_bad_scm &>/dev/null
    fi
}

##
# Help
#
function helpme() {
cat<<EOF
Gello inline build system (c) CyanogenMod 2015

Usage: ./gello_build.sh <flags>
flags:
    -v            = Verbose mode on
    -h            = Show this message
    --help        = Show this message
    --force-clean = Removes synced swe sources
    --no-sync     = Does the build but does not update from caf
EOF
}

##
# Main
#

# Verbose mode
if [ "$1" == "-v" ] || [ "$2" == "-v" ]; then
    VERBOSE=true
fi

if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    helpme
    exit 0
fi

# sth like "make -B"
if [ "$1" == "--force-clean" ]; then
    make_clean
    if [ "$?" == 0 ]; then
        echo "Build environment cleaned!"
    else
        echo "Something went wrong while removing build environment"
    fi
fi

# Check if running for the first time
if [ ! -f "$DONE_FILE" ]; then
    if [ ! -x $(which gclient) ]; then
        cd $TOP_GELLO/../tools
        echo "Depot tool not found! Installing..."
        if [ "$VERBOSE" == true ]; then
            git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
        else
            git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git &> /dev/null
        fi
        export PATH=$PATH:$TOP_GELLO/tools
        cd $TOP_GELLO/..
    fi
    echo "First time for Gello!\nThis will download a large amount of data!! \
          It can take a LOT of time.\nIf you want a prebuilt stop this and \
          'export WITH_GELLO_SOURCE=false'"
fi

# Sync
get_ready $1 $2

# Create
gimme_my_gello
