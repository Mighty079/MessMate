@echo off
title MessMate - Install & Run on Emulator
echo =======================================================
echo              MESSMATE EMULATOR DEPLOYER
echo =======================================================
echo.

set ADB_PATH="C:\Users\krish\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set APK_PATH="C:\Users\krish\Desktop\MessMate\app\build\outputs\apk\debug\app-debug.apk"

echo Checking for connected emulator/devices...
%ADB_PATH% devices

echo.
echo Installing APK to your emulator...
%ADB_PATH% install -r %APK_PATH%
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Installation failed. Please ensure your emulator is running and online.
    goto end
)

echo.
echo Launching MessMate application...
%ADB_PATH% shell am start -n com.example.messmate/.MainActivity
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Failed to start application on the device.
    goto end
)

echo.
echo [SUCCESS] MessMate is running on your emulator!
echo.

:end
pause
