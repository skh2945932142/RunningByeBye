@echo off
setlocal enabledelayedexpansion

REM RunningByeBye release APK build script for Windows.
REM App-only mode: if mobile\mobile.go is missing, reuse android\app\libs\mobile.aar.

cd /d "%~dp0"

echo [1/5] Checking environment...
where java >nul 2>&1 || (echo ERROR: Java not found && exit /b 1)

if "%ANDROID_HOME%"=="" (
    for /f "tokens=2 delims==" %%A in ('findstr /B "sdk.dir=" "android\local.properties" 2^>nul') do (
        set "ANDROID_HOME=%%A"
    )
    if not "!ANDROID_HOME!"=="" (
        set "ANDROID_HOME=!ANDROID_HOME:\:=:!"
        set "ANDROID_HOME=!ANDROID_HOME:\\=\!"
        set "ANDROID_HOME=!ANDROID_HOME:/=\!"
    )
)
if "%ANDROID_HOME%"=="" (
    echo ERROR: ANDROID_HOME is not set and android\local.properties has no sdk.dir
    exit /b 1
)
if not exist "%ANDROID_HOME%" (
    echo ERROR: Android SDK directory does not exist: %ANDROID_HOME%
    exit /b 1
)

echo [2/5] Preparing mobile AAR...
if exist "mobile\mobile.go" (
    where go >nul 2>&1 || (echo ERROR: Go source exists but Go was not found && exit /b 1)

    go install golang.org/x/mobile/cmd/gomobile@latest
    if errorlevel 1 exit /b 1
    go install golang.org/x/mobile/cmd/gobind@latest
    if errorlevel 1 exit /b 1
    for /f "tokens=*" %%A in ('go env GOPATH') do set "GO_PATH=%%A"
    set "PATH=%PATH%;%GO_PATH%\bin"
    gomobile init
    if errorlevel 1 exit /b 1

    set "HAS_NDK="
    if exist "%ANDROID_HOME%\ndk-bundle\meta\platforms.json" set "HAS_NDK=1"
    if exist "%ANDROID_HOME%\ndk" (
        for /d %%D in ("%ANDROID_HOME%\ndk\*") do (
            if exist "%%D\meta\platforms.json" set "HAS_NDK=1"
        )
    )
    if not defined HAS_NDK (
        echo ERROR: Android NDK not found under %ANDROID_HOME%
        echo Install it in Android Studio SDK Manager or run: sdkmanager "ndk;27.2.12479018"
        exit /b 1
    )

    if not exist "android\app\libs" mkdir "android\app\libs"
    set "BOOTCLASSPATH=%ANDROID_HOME%\platforms\android-35\android.jar"
    gomobile bind -androidapi 24 -bootclasspath "!BOOTCLASSPATH!" -target=android -o android\app\libs\mobile.aar RunningByeBye/mobile
    if errorlevel 1 exit /b 1
    echo mobile.aar generated from Go source
) else (
    if not exist "android\app\libs\mobile.aar" (
        echo ERROR: mobile\mobile.go is missing and android\app\libs\mobile.aar was not found
        exit /b 1
    )
    echo mobile\mobile.go not found; using existing android\app\libs\mobile.aar
)

echo [3/5] Refreshing point assets...
if not exist "data\points" (
    echo ERROR: data\points was not found
    exit /b 1
)
if exist "android\app\src\main\assets\points" (
    rmdir /S /Q "android\app\src\main\assets\points"
)
mkdir "android\app\src\main\assets\points"
xcopy /E /I /Y "data\points\*" "android\app\src\main\assets\points\"
if errorlevel 1 (
    echo ERROR: Failed to copy point assets
    exit /b 1
)

echo [4/5] Running Android unit tests...
cd android
call gradlew.bat :app:testDebugUnitTest
if errorlevel 1 exit /b 1

echo [5/5] Building release APK...
call gradlew.bat :app:assembleRelease
if errorlevel 1 exit /b 1

echo.
echo =============================================
echo Build succeeded.
echo APK path: android\app\build\outputs\apk\release\app-release.apk
echo =============================================

endlocal
