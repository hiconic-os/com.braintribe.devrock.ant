#!/bin/bash
################################################################################
# Builds the devrock-ant-tasks and then updates the libraries in [ANT_HOME]/lib.
# The "toant" target in the build.xml also updates the lib folder, but can't
# delete old libraries.
################################################################################

if [ -z ${ANT_HOME+x} ]
then
	echo 'ANT_HOME is not set!'
	exit 1
fi

echo 'Building devrock-ant-tasks ...'
ant install
echo 'Built devrock-ant-tasks.'

if [ -d ${ANT_HOME}/lib_backup ]; then
	echo 'Deleting old backup of ANT library folder ...'
	rm -r ${ANT_HOME}/lib_backup
fi

echo 'Creating backup of ANT library folder ...'
cp -ar ${ANT_HOME}/lib ${ANT_HOME}/lib_backup

echo 'Deleting old devrock-ant-tasks libs in ANT library folder ...'
rm ${ANT_HOME}/lib/bt.*.jar

echo 'Copying new devrock-ant-tasks libs to ANT library folder ...'
#TODO handle paths with spaces (simple quotes didn't work for me)
unzip -q dist/assembled/devrock-ant-tasks.zip -d ${ANT_HOME}/lib

echo 'Successfully updated ANT library folder.'
