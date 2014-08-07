#!/bin/sh

## You need to set env variables : JAVA_HOME

if [ "x${JAVA_HOME}" = "x" ]
then
   echo JAVA_HOME not defined. Must be defined to build java apps.
   exit
fi

echo cleaning...

rm -rf ./build/classes

mkdir -pv ./build/classes
javac -cp "$JAVA_HOME/bin/tools.jar:./lib/*" -d ./build/classes ./src/main/java/com/vmware/vchs/publicapi/samples/*.java