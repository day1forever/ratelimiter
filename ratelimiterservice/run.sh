#!/bin/bash
set -eux  # Exit on error
set -o pipefail  # Fail a pipe if any sub-command fails.

# This is the main launch script for launching the service on desktop or docker container.

VERSION=1.10

# This allows running this script without having to be in this script directory,
# e.g., you can run the script using /some/workspace/path/service/run.sh or
# ./path/service/run.sh
cd "$(dirname "$0")"

# Configuration settings
JAR="target/ratelimiter-service-*\.jar"
DEFAULT_DEBUG_PORT=5005
DESKTOP_LOCATION="desktop"
DEBUG_COMMAND="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address="

# These two files are used to detect current env.
# If they are not present, we will assume the service is running locally and default config will be used
REGION_FILE="/etc/region"
AD_FILE="/etc/availability-domain"
REALM_FILE="/etc/identity-realm"

DEBUG=""
CFG=""
JAR_FILE=$(ls $JAR | grep -v "sources\.jar$" | grep -v "javadoc\.jar$" 2> /dev/null)
RED='\033[0;31m'
ORANGE='\033[0;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

function usage() {
  echo "run.sh version: $VERSION"
  echo "Launches the server."
  echo "Usage: ./run.sh <config file> --debug=PORT"
  echo
  echo "Options:"
  echo "--debug|d             Launch the JVM in remote debugging mode listening"
  echo "--debug=<port>        to the specified port or else the default port of"
  echo "                      5005."
  echo "--help                Prints the help screen"
  echo
  echo "Examples:"
  echo "./run.sh"
  echo "./run.sh --debug"
  echo "./run.sh --debug=5005"
  echo "./run.sh config/test.conf"
  echo "./run.sh config/test.conf --debug"
  echo "./run.sh config/test.conf --debug=5005"
  echo "./run.sh --debug=5005 config/test.conf"
  exit 0
}

function die() {
  printf "${RED}%s\n${NC}" "$1" >&2
  exit 1
}

function warn() {
  printf "${ORANGE}%s\n${NC}" "$1"
}

function info() {
  printf "${GREEN}%s\n${NC}" "$1"
}

for i in "$@"
do
case $i in
  -d|--debug)
    DEBUG="$DEBUG_COMMAND$DEFAULT_DEBUG_PORT"
    shift
    ;;
  -d=*|--debug=*)
    DEBUG="$DEBUG_COMMAND${i#*=}"
    shift
    ;;
  -h|--help)
    usage
    shift
    ;;
  *)
    # If the arg doesn't start with a '-', assume to be the config file
    [[ "$i" != -* ]] && CFG="$i" ; shift
    # Otherwise it's an unknown option
    ;;
esac
done

info "run.sh executed from $(pwd)"

# Verify only a single jar was found
if [[ `echo -n "$JAR_FILE" | grep -c '^'` > 1 ]] ; then
  die "Only a single jar is expected, but found more than one: $JAR_FILE"
fi

# Verify the jar exists
if [[ ! -f "$JAR_FILE" ]] ; then
  die "jar file '$JAR' not found.  Did you build? If not, run: mvn clean install"
fi

# If supplied, make sure the config file exists
if [[ "$CFG" && ! -f "$CFG" ]] ; then
   die "Config file '$CFG' does not exist or is not a file"
fi

RUNNING_LOCALLY=true
if [[ -e "$REGION_FILE" && -e "$AD_FILE" ]]; then
  RUNNING_LOCALLY=false
fi

# Figure out which region we are in.
REGION=""
STAGE="dev"
if [[ $RUNNING_LOCALLY == false ]]; then
  REGION=$(echo $(cat "$REGION_FILE") | tr '[:upper:]' '[:lower:]')
  AVAILABILITY_DOMAIN=$(echo $(cat "$AD_FILE") | tr '[:upper:]' '[:lower:]')
  REALM=$(echo $(cat "$REALM_FILE") | tr '[:upper:]' '[:lower:]')

  info "Found region:  $REGION "
  info "Found availability domain: $AVAILABILITY_DOMAIN "
  info "Found realm:  $REALM "

  STAGE="prod"
  LOCATION=$(echo "$REGION-$AVAILABILITY_DOMAIN")
else
  # Not in a Docker container. Assumed to be running on a developers desktop.
  LOCATION="$DESKTOP_LOCATION"
  warn "Running service locally. Assuming to be a developer desktop.  Defaulting to location $LOCATION"
fi


if [[ -n $CFG ]] ; then
    # If CFG provided, don't try other configs
    if [[ ! -f "$CFG" ]] ; then
       die "Config file '$CFG' does not exist or is not a file"
    fi
elif [[ ! -z "${ENV_OVERRIDE-}" ]] ; then
    # By setting the ENV_OVERRIDE environment variable, you can load a different conf file.
    # This is useful where you want to run a test stack in the same region as an existing stack
    CFG="target/config/$ENV_OVERRIDE.conf"
    info "The ENV_OVERRIDE environment variable is set. Will use $ENV_OVERRIDE to build the conf file"
    if [[ ! -f "$CFG" ]] ; then
       die "The ENV_OVERRIDE file '$CFG' does not exist or is not a file"
    fi
else
    # Look for matching config files in this order:
    if [[ ! -z "${USER-}" ]] ; then
        USER_CFG="target/config/$USER.conf"         # Ex: rroller.conf
    else
        USER_CFG=""
    fi
    LOCATION_CFG="target/config/$LOCATION.conf" # Ex: desktop.conf

    info "Looking for configs in: (user: $USER_CFG), (location: $LOCATION_CFG)"
    if [[ $LOCATION == "$DESKTOP_LOCATION" && -f "$USER_CFG" ]] ; then
        CFG="$USER_CONFIG"
    else
        CFG="$LOCATION_CFG"
    fi
fi

info "Using config file $(pwd)/$CFG"

#figure out which environment file to run.
cd target/environment-config/
if [[ $LOCATION == "$DESKTOP_LOCATION" ]] ; then
    ENV_FILE="$DESKTOP_LOCATION.env"
else
    ENV_FILE="base.env"
fi
info "Using environment config file $(pwd)/$ENV_FILE"
source $ENV_FILE
cd ../..

CMD="java $DEBUG -Djava.security.egd=file:///dev/urandom $JAVA_MEMORY_SETTINGS $JVM_GC_OPTIONS $TRUST_STORE_SETTINGS -jar $JAR_FILE server $CFG"

# Disable core dumps by default. On production (stable) machines, we should not be collecting dumps by default.
# You run the risk of running out of docker space if core dumps are enabled.
# Remove / comment this line if you intentionally want to enable dumps (say for an unstable environment).
ulimit -c 0

# Now execute it
info "Executing: $CMD"
exec $CMD