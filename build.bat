@echo off

setlocal
set SAMPLEDIR=.\src\main\java\com\vmware\vchs\publicapi\samples
set SAMPLEJARDIR=%CD%\lib

if NOT DEFINED JAVAHOME (
   @echo JAVAHOME not defined. Must be defined to build java apps.
   goto END
)

@echo cleaning..
	rmdir /s /q build


:SETENV
set PATH=%JAVAHOME%\bin;%PATH%

set LOCALCLASSPATH=%CD%\lib;

for %%i in ("lib\*.jar") do call lcp.bat %CD%\%%i

@echo Compiling samples and adding to build\classes folder.

mkdir .\build\classes
%JAVAHOME%\bin\javac -XDignore.symbol.file -d ./build/classes -classpath "%LOCALCLASSPATH%" %SAMPLEDIR%\*.java

cd ..

:END
@echo Done.

