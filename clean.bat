@echo off

set SAMPLEDIR=.\src\main\java\com\vmware\vchs\publicapi\samples

cd %SAMPLEDIR%


if exist "*.class" del /F "*.class"


cd ..\..\..\..\..\..\..\..



:CLEANEND

@echo on