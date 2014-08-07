@echo off
@REM you need to set env variables : JAVAHOME, or modify the 2 values here

set SAMPLEDIR=.

setlocal

:SETENV
set PATH=%JAVAHOME%\bin;%PATH%
set LOCALCLASSPATH=%CD%\lib;%CD%\build\classes
for %%i in ("lib\*.jar") do call lcp.bat %CD%\%%i
set LOCALCLASSPATH=%LOCALCLASSPATH%;%CLASSPATH%

:next
if [%1]==[] goto argend   
   set ARG=%ARG% %1   
   shift
   goto next
:argend

:DORUN

%JAVAHOME%\bin\java -cp "%LOCALCLASSPATH%;.\src\main\java" -Xmx256M %ARG%
popd

endlocal

:END