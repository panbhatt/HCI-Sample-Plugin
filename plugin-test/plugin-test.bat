 @echo off

@REM HCI plugin test harness startup script

@REM This script requires version 1.8 of the Java Runtime Environment (JRE).
@REM In order to run, Java must be in the PATH environment variable.

@REM For more information, view the README.txt located in the cli directory.

java -version 2> tmp_java_version.txt
IF %ERRORLEVEL% NEQ 0 (
  ECHO This script requires version 1.8 of the Java Runtime Environment.
  DEL tmp_java_version.txt
  EXIT /B 1
)
SET /p JAVA_VERSION= < tmp_java_version.txt
DEL tmp_java_version.txt
SET JAVA_VERSION=%JAVA_VERSION:~14,3%
IF /I %JAVA_VERSION% LSS 1.8 (
    ECHO This script requires version 1.8 of the Java Runtime Environment.
    EXIT /B 1
)

setlocal enabledelayedexpansion

set HCI_PLUGIN_TEST_BASEDIR=%~dp0
set HCI_PLUGIN_TEST_CP=%HCI_PLUGIN_TEST_BASEDIR%..\lib\*;%JAVA_HOME%/jre/lib/ext/sunjce_provider.jar
set HCI_PLUGIN_TEST_EXT_DIR=%HCI_PLUGIN_TEST_BASEDIR%..\lib\sdk
set LOGENV=-Dlogback.configurationFile="%HCI_PLUGIN_TEST_BASEDIR%logback.xml"
set DEBUG=
set JAR=
set JARPATH=

:loop
IF NOT "%1"=="" (
    IF "%1"=="-d" (
        set DEBUG=-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5903 -Xdebug
    )
    IF "%1"=="--debug" (
        set DEBUG=-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5903 -Xdebug
    )
	IF "%1"=="-j" (
        set JAR=%2
		set JARPATH=%~dp2
    )
	IF "%1"=="--jar" (
        set JAR=%2
		set JARPATH=%~dp2
    )
    SHIFT
    GOTO :loop
)

IF NOT "%JAR%"=="" (
	
	set HCI_PLUGIN_TEST_CP=%JAR%;%HCI_PLUGIN_TEST_CP%;
	set HCI_PLUGIN_TEST_EXT_DIR=%JARPATH%;%HCI_PLUGIN_TEST_EXT_DIR%
)

IF NOT DEFINED JAVA_HOME (
    ECHO Warning: JAVA_HOME is not currently defined. Using system PATH.
    set EXTENV=-Djava.ext.dirs="%HCI_PLUGIN_TEST_EXT_DIR%"
)
IF DEFINED JAVA_HOME (
    ECHO Using java from: %JAVA_HOME%
    REM set EXTENV=-Djava.ext.dirs="%HCI_PLUGIN_TEST_EXT_DIR%;%JAVA_HOME%\lib\ext"
	set EXTENV=-Djava.ext.dirs="%HCI_PLUGIN_TEST_EXT_DIR%"
)

set JAVA_OPTIONS=%JAVA_OPTIONS% %LOGENV% %EXTENV% %DEBUG%


java %JAVA_OPTIONS% -cp "%HCI_PLUGIN_TEST_CP%" com.hds.ensemble.sdk.plugin.test.PluginTestHarness %*
