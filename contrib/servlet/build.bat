@echo off

echo Fop Build System
echo ----------------

if "%JAVA_HOME%" == "" goto error

set LIBDIR=../../lib
set LOCALCLASSPATH=%JAVA_HOME%\lib\tools.jar;%JAVA_HOME%\lib\classes.zip;%LIBDIR%\ant-1.4.1.jar;%LIBDIR%\buildtools.jar;%LIBDIR%\xercesImpl-2.0.1.jar;%LIBDIR%\xml-apis.jar;%LIBDIR%\xalan-2.3.1.jar

set ANT_HOME=%LIBDIR%

echo Building with classpath %LOCALCLASSPATH%

echo Starting Ant...

%JAVA_HOME%\bin\java.exe -Dant.home=%ANT_HOME% -classpath "%LOCALCLASSPATH%" org.apache.tools.ant.Main %1 %2 %3 %4 %5

goto end

:error

echo ERROR: JAVA_HOME not found in your environment.
echo Please, set the JAVA_HOME variable in your environment to match the
echo location of the Java Virtual Machine you want to use.

:end

rem set LOCALCLASSPATH=

