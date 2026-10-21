#!/usr/bin/env sh
##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

DEFAULT_JVM_OPTS=""

PRG="$0"
while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`
[ -z "$APP_HOME" ] && APP_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`

exec "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
