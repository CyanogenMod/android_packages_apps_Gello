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

# Flags bools
VERBOSE=false
GOFAST=false
NOSYNC=false

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

    # Check if we were asked to not sync
    if [ "$NOSYNC" != false ] && [ "$GOFAST" != false ]; then
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
            if [ "$?" != 0 ]; then
                echo "Something went wrong while syncing!"
                exit 1
            fi
        fi
    fi

    # Move src/swe/browser to src/swe/browser-caf as backup/timesaver for future syncs
    # We can't link (build will fail), so we'll just copy gello to src/swe/browser
    if [ -d "$BUILD_BROWSER" ]; then
        mv $BUILD_BROWSER $CAF_BROWSER
    fi
    cp -r $GELLO_SRC $BUILD_BROWSER
    if [ ! -f $DONE_FILE ]; then
        touch $DONE_FILE
    fi
}

##
# Gello compilation
#
function gimme_my_gello() {
    local COMPILED_APK=$SRC_DIR/out/Release/apks/SWE_AndroidBrowser.apk
    READY_APK=$TOP_GELLO/Gello.apk

    cd $SRC_DIR
    . build/android/envsetup.sh

    if [ "$GOFAST" != true ]; then
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
            return 3
        fi
    else
        echo "You are in a hurry! Skipping environment updates. The build may fail!"
    fi

    echo "Compiling apk with ninja..."
    # Always show build output even if verbose is false
    ninja -C out/Release swe_android_browser_apk

    if [ "$?" == 0 ]; then
        if [ -f "$COMPILED_APK" ]; then
            # If found, remove previous compiled apk
            if [ -f "$READY_APK" ]; then
                rm -f $READY_APK
            fi
            # Copy new apk to its new home.
            cp $COMPILED_APK $READY_APK
            # A green message to comunicate we did it
            echo "$(tput setaf 2)Done! Apk located to $READY_APK$(tput sgr reset)"
            return 0
        else
            echo "Unable to find output (Excepted to be at $COMPILED_APK)"
            return 1
        fi
    else
        cat /tmp/gello_output
        echo "Build failed!!"
        if [ "$GOFAST" == true ]; then # If build failed and GOFAST is true, hint to update runhooks
            echo "You may want to not use --fast flag next time."
        fi
        return 3
    fi
}

##
# Cleaner
#
function make_clean() {
    # Remove everything
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
# Setup
# See https://www.codeaurora.org/xwiki/bin/Chromium+for+Snapdragon/Setup
#
function firsttime() {
    if [ ! -x $(which gclient) ]; then
        # if we don't have depot_tools clone them into a "gitignored" directory
        if [ ! -d "$TOP_GELLO/../tools" ]; then
            mkdir $TOP_GELLO/../tools
        fi
        cd $TOP_GELLO/../tools
        echo "Depot tool not found! Installing..."
        if [ "$VERBOSE" == true ]; then
            git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
        else
            git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git &> /dev/null
        fi
        export PATH=$PATH:$TOP_GELLO/../tools
        cd $TOP_GELLO/..
    fi
    # Sync
    if [ ! -f "$DONE_FILE" ]; then
        echo "$(tput setaf 3)First time for Gello!\nThis will download a large amount of data!! \
              It can take a LOT of time.\nIf you want a prebuilt stop this and \
              'export WITH_GELLO_SOURCE=false'$(tput sgr reset)"
    fi
    get_ready
    cd $TOP_GELLO/src/build
    #chmod +x install-build-deps.sh && ./install-build-deps.sh # shouldn't be needed for us
    chmod +x install-build-deps-android.sh && ./install-build-deps-android.sh
    if [ "$?" == 0 ]; then
        echo "Setup completed sucesfully!"
    else
        echo "Unable to complete the setup!"
        return 5
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
    -h            = Show this message
    -v            = Verbose mode on
    --fast        = Skip sync and runhooks, useful for testing
    --force-clean = Removes synced swe sources
    --help        = Show this message
    --mp          = Compile and push (It first try with --flag, if build fails
                    it retries without)
    --no-sync     = Does the build but does not update from caf
    --setup       = Useful when you set up the environment for the first time,
                    but requires user inputs (EULA licenses and password to
                    install required packages)
    --verbose     = Verbose mode on
EOF
}

##
# Main
#

# Check flags
if [ "$1" == "-v" ] || [ "$2" == "-v" ] ||
   [ "$3" == "-v" ] || [ "$4" == "-v" ] ||
   [ "$1" == "--verbose" ] || [ "$2" == "--verbose" ] ||
   [ "$3" == "--verbose" ] || [ "$4" == "--verbose" ]; then
    VERBOSE=true
fi

if [ "$1" == "--fast" ] || [ "$2" == "--fast" ] ||
   [ "$3" ==  "--fast" ] || [ "$4" == "--fast" ]; then
    GOFAST=true
fi


if [ "$1" == "--no-sync" ] || [ "$2" == "--no-sync" ] ||
   [ "$3" == "--no-sync" ] || [ "$4" == "--no-sync" ]; then
    NOSYNC=true
fi

if [ "$1" == "--mp" ] || [ "$2" == "--mp" ] ||
   [ "$3" == "--mp" ] || [ "$4" == "--mp" ]; then
    gimme_my_gello
    if [ "$?" != 0 ]; then
        exit 1
    fi
    adb wait-for-device
    adb install -r $READY_APK
    if [ "$?" == 0 ]; then
        exit 0
    else
        exit $?
    fi
fi


if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    helpme
    exit 0
fi

if [ "$1" == "--setup" ]; then
    firsttime
    gimme_my_gello
    if [ "$?" == 0 ]; then
        exit 0
    fi
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
    echo "$(tput setaf 3)First time for Gello!\nThis will download a large amount of data!! \
          It can take a LOT of time.\nIf you want a prebuilt stop this and \
          'export WITH_GELLO_SOURCE=false'$(tput sgr reset)"
fi

# Sync
get_ready $1 $2

# Create
gimme_my_gello

if [ "$?" == 0 ]; then
    exit 0
else
    exit $?
fi
