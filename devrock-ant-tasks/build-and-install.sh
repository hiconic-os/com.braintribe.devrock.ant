#!/bin/bash

if [ -z $ANT_HOME ] ; then
    echo "missing ANT_HOME variable"
    exit
fi

CANONIZED_ANT_HOME="${ANT_HOME//\\//}"

echo $CANONIZED_ANT_HOME

rm -fv $CANONIZED_ANT_HOME/lib/bt.*.jar

find dist/lib -name "*.jar" -exec basename \{} .jar \; | xargs -n 1 -I {} cp -v "dist/lib/{}.jar" "$CANONIZED_ANT_HOME/lib/bt.{}.jar"
