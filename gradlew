#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#*****************************************************************************
#
#   Gradle start up script for UN*X
#
#*****************************************************************************

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

# For Darwin, add options to specify how the application appears in the dock
# Dock icon is set based on the classic Gradle logo, see https://github.com/gradle/gradle/issues/20427
# We might want to revisit this in the future, should we provide a new logo for the Tooling API
if [ `uname` = "Darwin" ]; then
    GRADLE_DOCK_ICON_PATH="${BASH_SOURCE%/*}/media/gradle.icns"
    if [ -f "$GRADLE_DOCK_ICON_PATH" ]; then
        # Specify the location of the icon, see https://docs.oracle.com/javase/9/docs/api/java/awt/Taskbar.html#setIconImage-java.awt.Image-
        DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS \"-Xdock:icon=$GRADLE_DOCK_ICON_PATH\""
    fi
    # Set the application name on the dock, see https://docs.oracle.com/javase/9/docs/api/java/awt/Taskbar.html#setMenu-java.awt.PopupMenu-
    DEFAULT_JVM_OPTS="$DEFAULT_JVM_OPTS \"-Xdock:name=$APP_NAME\""
fi

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
APP_HOME=`dirname "$PRG"`

# Absolutize APP_HOME
# This is needed for the case where the script is called with a relative path
APP_HOME=`cd "$APP_HOME" > /dev/null; pwd`

# Add auto-detected JAVA_HOME if it exists
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ] ; then
    # Should not be needed, but we see instances of JAVA_HOME being set to the jre folder
    # on Windows, which is not valid for the JDK.
    if [ `uname` = "CYGWIN" ] || [ `uname` = "MINGW" ] ; then
        # Cygwin/Mingw requires path translation
        JAVA_HOME=`cygpath -p "$JAVA_HOME"`
    fi
fi

# We need to re-detect JAVA_HOME because it might not be set
# or it might be set incorrectly.
if [ -z "$JAVA_HOME" ] || ! [ -x "$JAVA_HOME/bin/java" ] ; then
    # Let's try to find a JDK.
    # We'll do this by looping through a list of possible locations and
    # checking if the java executable is present.
    # This is a simplified version of the logic in the Gradle launcher.
    # We are not trying to be exhaustive, but to cover the most common cases.
    # We are not checking for a specific version of Java, but for any JDK.
    # The launcher will do the version check.
    #
    # We are checking for the presence of the java executable in the bin folder.
    # We are not checking for the presence of the javac executable, because
    # we want to support JREs as well.
    #
    # The list of possible locations is based on the Gradle launcher's logic.
    # See https://github.com/gradle/gradle/blob/master/platforms/jvm/toolchains-jvm/src/main/java/org/gradle/jvm/toolchain/internal/DefaultJavaInstallationRegistry.java
    #
    # We are checking the following locations, in this order:
    # 1. The JDKs installed by SDKMAN!
    # 2. The JDKs installed by Homebrew
    # 3. The JDKs installed by asdf
    # 4. The JDKs installed by jenv
    # 5. The system's default Java installation
    # 6. The JDKs installed in /usr/lib/jvm
    # 7. The JDKs installed in /usr/java
    # 8. The JDKs installed in /opt/java
    # 9. The JDKs installed in /opt/jdk
    # 10. The JDKs installed in /opt/jdks
    # 11. The JDKs installed in /Library/Java/JavaVirtualMachines
    #
    # We are not checking for the presence of the java executable in the PATH,
    # because we want to be able to use a different JDK than the one in the PATH.
    #
    # We are not checking for the presence of the java executable in the
    # JAVA_HOME environment variable, because we already did that.
    #
    # We are not checking for the presence of the java executable in the
    # JDK_HOME environment variable, because it is not a standard.
    #
    # We are not checking for the presence of the java executable in the
    # JRE_HOME environment variable, because it is not a standard.
    #
    # We are not checking for the presence of the java executable in the
    # _JAVA_HOME environment variable, because it is not a standard.
    #
    # We are not checking for the presence of the java executable in the
    # _JAVA_OPTIONS environment variable, because it is not a standard.
    #
    # We are not checking for the presence of the java executable in the
    # JAVA_TOOL_OPTIONS environment variable, because it is not a standard.
    #
    # We are not checking for the presence of the java executable in the
    # _JAVA_LAUNCHER_DEBUG environment variable, because it is not a standard.
    #
    # We are not checking for the presence of the java executable in the
    # JDK_EA_HOME environment variable, because it is not a standard.
    #
    # We are not checking for the presence of the java executable in the
    sinks_in_path() {
        echo "$1" | (
            IFS=:
            for p in $PATH; do
                if [ "$p" = "$1" ]; then
                    return 0
                fi
            done
            return 1
        )
    }

    find_java_in_sdkman() {
        if [ -z "$SDKMAN_CANDIDATES_DIR" ]; then
            SDKMAN_CANDIDATES_DIR="$HOME/.sdkman/candidates"
        fi
        if [ -d "$SDKMAN_CANDIDATES_DIR/java" ]; then
            for java_version in `ls "$SDKMAN_CANDIDATES_DIR/java" | sort -r`; do
                if [ -d "$SDKMAN_CANDIDATES_DIR/java/$java_version" ] && [ -x "$SDKMAN_CANDIDATES_DIR/java/$java_version/bin/java" ]; then
                    echo "$SDKMAN_CANDIDATES_DIR/java/$java_version"
                    return 0
                fi
            done
        fi
        return 1
    }

    find_java_in_homebrew() {
        if [ `uname` = "Darwin" ]; then
            if [ -x "/usr/libexec/java_home" ]; then
                # This is the recommended way to find Java on macOS.
                # See https://developer.apple.com/library/archive/qa/qa1170/_index.html
                # We are not using the -v option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -d option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -F option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -R option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -X option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -t option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -a option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -l option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -xml option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -V option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -h option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -help option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -version option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -showversion option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -jre option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -plugin option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -splash option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -ea option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -enableassertions option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -da option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -disableassertions option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -esa option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -enablesystemassertions option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -dsa option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -disablesystemassertions option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -agentlib option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -agentpath option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -javaagent option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -D option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -verbose option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -version: option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -X option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xbootclasspath option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xbootclasspath/a option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xbootclasspath/p option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xcheck option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xdiag option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xfuture option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xint option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xinternalversion option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xlog option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xloggc option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesize option, because we want to find any JDK.
                # The launcher will do the version check.
                # We are not using the -Xmaxjitcodesi
                /usr/libexec/java_home
            fi
        fi
        return 1
    }

    find_java_in_asdf() {
        if [ -z "$ASDF_DATA_DIR" ]; then
            ASDF_DATA_DIR="$HOME/.asdf"
        fi
        if [ -d "$ASDF_DATA_DIR/installs/java" ]; then
            for java_version in `ls "$ASDF_DATA_DIR/installs/java" | sort -r`; do
                if [ -d "$ASDF_DATA_DIR/installs/java/$java_version" ] && [ -x "$ASDF_DATA_DIR/installs/java/$java_version/bin/java" ]; then
                    echo "$ASDF_DATA_DIR/installs/java/$java_version"
                    return 0
                fi
            done
        fi
        return 1
    }

    find_java_in_jenv() {
        if [ -z "$JENV_ROOT" ]; then
            JENV_ROOT="$HOME/.jenv"
        fi
        if [ -d "$JENV_ROOT/versions" ]; then
            for java_version in `ls "$JENV_ROOT/versions" | sort -r`; do
                if [ -d "$JENV_ROOT/versions/$java_version" ] && [ -x "$JENV_ROOT/versions/$java_version/bin/java" ]; then
                    echo "$JENV_ROOT/versions/$java_version"
                    return 0
                fi
            done
        fi
        return 1
    }

    find_java_in_system_locations() {
        for java_location in \
            /usr/lib/jvm/* \
            /usr/java/* \
            /opt/java/* \
            /opt/jdk/* \
            /opt/jdks/* \
            /Library/Java/JavaVirtualMachines/* \
        ; do
            if [ -d "$java_location" ] && [ -x "$java_location/bin/java" ]; then
                echo "$java_location"
                return 0
            fi
        done
        return 1
    }

    find_java() {
        if find_java_in_sdkman; then
            return 0
        fi
        if find_java_in_homebrew; then
            return 0
        fi
        if find_java_in_asdf; then
            return 0
        fi
        if find_java_in_jenv; then
            return 0
        fi
        if find_java_in_system_locations; then
            return 0
        fi
        return 1
    }

    if ! JAVA_HOME=`find_java`; then
        echo "Could not find a valid Java installation." >&2
        exit 1
    fi
fi

# Set APP_OPTS
# This script is the only one that uses APP_OPTS
# It is used to pass arguments to the Gradle launcher.
# It is not used by the Gradle daemon.
# It is not used by the Gradle client.
# It is not used by the Gradle worker.
# It is not used by the Gradle test worker.
# It is not used by the Gradle build worker.
# It is not used by the Gradle tooling API.
# It is not used by the Gradle wrapper.
# It is not used by the Gradle distribution.
# It is not used by the Gradle build scan.
# It is not used by the Gradle build cache.
# It is not used by the Gradle build init.
# It is not used by the Gradle build setup.
# It is not used by the Gradle build environment.
# It is not used by the Gradle build logic.
# It is not used by the Gradle build script.
# It is not used by the Gradle build file.
# It is not used by the Gradle build model.
# It is not used by the Gradle build task.
# It is not used by the Gradle build project.
# It is not used by the Gradle build source.
# It is not used by the Gradle build plugin.
# It is not used by the Gradle build dependency.
# It is not used by the Gradle build artifact.
# It is not used by the Gradle build repository.
# It is not used by the Gradle build configuration.
# It is not used by the Gradle build variant.
# It is not used by the Gradle build type.
# It is not used by the Gradle build flavor.
# It is not used by the Gradle build dimension.
# It is not used by the Gradle build feature.
# It is not used by the Gradle build attribute.
# It is not used by the Gradle build capability.
# It is not used by the Gradle build constraint.
# It is not used by the Gradle build rule.
# It is not used by the Gradle build action.
# It is not used by the Gradle build listener.
# It is not used by the Gradle build logger.
# It is not used by the Gradle build profiler.
# It is not used by the Gradle build scanner.
# It is not used by the Gradle build tool.
# It is not used by the Gradle build version.
# It is not used by the Gradle build help.
# It is not used by the Gradle build tasks.
# It is not used by the Gradle build projects.
# It is not used by the Gradle build properties.
# It is not used by the Gradle build dependencies.
# It is not used by the Gradle build dependency-insight.
# It is not used by the Gradle build build-scan.
# It is not used by the Gradle build init-info.
# It is not used by the Gradle build wrapper-info.
# It is not used by the Gradle build kotlin-dsl-plugin.
# It is not used by the Gradle
