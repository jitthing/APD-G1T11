@echo off
echo ========================================
echo Building Password Cracker JAR...
echo ========================================
cd code
call mvn clean package
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b %ERRORLEVEL%
)
echo.
echo Copying JAR to root...
copy target\se301-1.1-SNAPSHOT-jar-with-dependencies.jar ..\run.jar
cd ..
echo.
echo ========================================
echo Build complete! JAR ready at run.jar
echo ========================================