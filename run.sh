#!/bin/sh

## you need to set env variables : JAVA_HOME

if [ "x${JAVA_HOME}" = "x" ]
then
   echo JAVA_HOME not defined. Must be defined to run java apps.
   exit
fi

# Define where is the java executable is
JAVA_CMD=java
if [ -d "$JAVA_HOME" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

echo running...

$JAVA_CMD -cp "$JAVA_HOME/bin/tools.jar:./lib/*:./build/classes/" -Xmx1024M $@