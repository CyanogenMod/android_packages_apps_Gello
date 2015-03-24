#!/bin/bash
#
# Copyright (c) 2013, The Linux Foundation. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
#     * Redistributions of source code must retain the above copyright
#      notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above
#      copyright notice, this list of conditions and the following
#      disclaimer in the documentation and/or other materials provided
#      with the distribution.
#    * Neither the name of The Linux Foundation nor the names of its
#      contributors may be used to endorse or promote products derived
#      from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
# WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
# ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
# BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
# BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
# WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
# OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
# IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
# Construct the XML entity for the &about; entity in
#  res/values/about.xml.
#
# Usage:
#  generate_about.sh [OPTIONS]
#
# Generates SWE Android Browser's About box XML resource and
#   emits manifest version name/code values
#
# Options:
#  --name : emit suitable manifest version name
#  --code : emit suitable manifest version code
#  --about : generate res/values/about.xml
#
#
me=${0##*/}
mydir=${0%/*}

# input/output files
VERSIONFILE=${mydir}/../VERSION
ABOUTFILE=${mydir}/../res/values/about.xml

# default
NAME=0
CODE=0
ABOUT=0
QUIET=0

# poor man's getopt()
[[ " ${*} " =~ " --name " ]] && NAME=1
[[ " ${*} " =~ " --code " ]] && CODE=1
[[ " ${*} " =~ " --quiet " ]] && QUIET=1
[[ " ${*} " =~ " --about " ]] && ABOUT=1

function warning()
{
    (( ${QUIET} )) || echo "Warning: ${me}: $@" >&2
}

function error()
{
    ret=$1
    shift
    echo "Error: ${me}: $@" >&2
    exit ${retval}
}

VERSIONINFO=$(cat ${VERSIONFILE}) || error 1 "couldn't read \"${VERSIONFILE}\""
VERSIONINFO=( ${VERSIONINFO//;/ } )

BASE_STRING=( ${VERSIONINFO[0]//=/ } )
BASE_STRING=( ${BASE_STRING[1]//./ } )
MAJOR=${BASE_STRING[0]}
MINOR=${BASE_STRING[1]}

BRANCH=( ${VERSIONINFO[1]//=/ } )
BRANCH=${BRANCH[1]}
BUILDID=unknown
VERSION=${MAJOR}.${MINOR}

# defaults
HASH=unknown

DATE=$(date) || warning "unable to get date"

# collect information about branch, version, etc. from CHROME_SRC
while [[ -d "${CHROME_SRC}" ]]
do
    pushd "${CHROME_SRC}" >/dev/null || error 1 "can't pushd ${CHROME_SRC}.."

    # this had better work..
    if ! HASH=$(git rev-parse --short HEAD)
    then
        HASH=unknown
        warning "${CHROME_SRC} apparently not under git?"
        break
    fi

    # collect branch and clean it up
    # tack on branch
    VERSION=${VERSION}.${BRANCH}

    # construct a version, start with a default
    MERGE_BASE=$(git merge-base remotes/origin/master "${HASH}") || warning "can't find a merge-base with master for hash \"$HASH\""

    # requires grafted tree and numeric branch name
    if [[ -n ${MERGE_BASE} ]]
    then
        BUILDID=$(git log --oneline ${MERGE_BASE}.. | wc -l)
        VERSION=${VERSION}.${BUILDID}
    else
        warning "using version ${VERSION}.. merge-base:\"${MERGE_BASE}\" branch: \"${BRANCH}\""
    fi

    popd >/dev/null || error 1 "popd from $CHROME_SRC failed?"
    break
done

if (( ${ABOUT} ))
then
   # add a support link, if configured
    if [[ -n ${SWE_APK_SUPPORT} ]]
    then
        CONTACT="Please help us make your experience better by contacting the
team at <a href=\"mailto:${SWE_APK_SUPPORT}\">${SWE_APK_SUPPORT}</a>\n"
    fi

    printf %s "<?xml version=\"1.0\" encoding=\"utf-8\"?>
<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">
   <!-- Text to display in about dialog -->
   <string name=\"about_text\" formatted=\"false\">
${CONTACT}
Version: ${VERSION}\n
Built: ${DATE}\n
Host: ${HOSTNAME}\n
User: ${USER}\n
Hash: ${HASH} (${BRANCH})\n
</string>
</resources>
" > "${ABOUTFILE}" || error 1 "could not write to ${ABOUTFILE}"
fi

(( ${NAME} )) && echo "${VERSION} (${HASH})"

# decimal-ify for printf
[[ -n ${MAJOR//[0-9]/} ]] && MAJOR=0
[[ -n ${MINOR//[0-9]/} ]] && MINOR=0
[[ -n ${BRANCH//[0-9]/} ]] && BRANCH=0
[[ -n ${BUILDID//[0-9]/} ]] && BUILDID=0

# This value should always be less than MAX int value "2147483647"
(( ${CODE} )) && printf "%4d%04d\n" $((${BRANCH})) $((${BUILDID}))


exit 0
