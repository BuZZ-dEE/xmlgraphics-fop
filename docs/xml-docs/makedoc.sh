#!/bin/sh
#This file should be executable


echo
echo "Converting Fop's xml documentation into a pdf file, creating html docs"
echo "----------------"
echo

if [ "$JAVA_HOME" = "" ] ; then
  echo "ERROR: JAVA_HOME not found in your environment."
  echo
  echo "Please, set the JAVA_HOME variable in your environment to match the"
  echo "location of the Java Virtual Machine you want to use."
  exit 1
fi

LIBDIR=../../lib
LOCALCLASSPATH=$JAVA_HOME/lib/tools.jar:$JAVA_HOME/lib/classes.zip:$LIBDIR/ant.jar:$LIBDIR/batik.jar:$LIBDIR/buildtools.jar:$LIBDIR/xerces-1.2.3.jar:$LIBDIR/xalan-2.0.0.jar:$LIBDIR/bsf.jar:$LIBDIR/avalon-framework-4.0.jar:$LIBDIR/logkit-1.0.jar:$LIBDIR/jimi-1.0.jar:$LIBDIR/stylebook.jar:$LIBDIR/../build/fop.jar
ANT_HOME=$LIBDIR

echo Building with classpath $LOCALCLASSPATH:$CLASSPATH
echo

echo Starting Ant...
echo

$JAVA_HOME/bin/java -Dant.home=$ANT_HOME -classpath "$LOCALCLASSPATH:$CLASSPATH" org.apache.tools.ant.Main $*


$JAVA_HOME/bin/java -classpath "$LOCALCLASSPATH:$CLASSPATH" org.apache.stylebook.StyleBook "targetDirectory=../html-docs/" fop.xml skins/xml.apache.org/
