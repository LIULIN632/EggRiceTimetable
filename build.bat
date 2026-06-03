@echo off
chcp 65001 >nul
echo ======================================
echo   蛋炒饭课程表 一键打包
echo ======================================

echo [1/2] 编译APK...
call gradlew assembleDebug
if %errorlevel% neq 0 (
    echo ❌ 编译失败，请检查错误信息
    pause
    exit /b 1
)

echo [2/2] 重命名APK...
call gradlew renameApk
if %errorlevel% neq 0 (
    echo ❌ 重命名失败
    pause
    exit /b 1
)

echo ======================================
echo ✅ 打包完成
for /f "tokens=*" %%i in ('dir /b /o-d 蛋炒饭课程表_v*.apk 2^>nul') do (
    echo 📦 %%i
    goto :done
)
:done
echo ======================================
pause
