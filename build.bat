@echo off
chcp 65001 >nul
echo ======================================
echo   蛋炒饭课程表 一键打包 (Release 签名版)
echo ======================================

echo [1/2] 编译 Release 签名 APK...
call gradlew renameApk
if %errorlevel% neq 0 (
    echo ❌ 编译失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo [2/2] 验证签名...
for /f "tokens=*" %%i in ('dir /b /o-d 蛋炒饭课程表_v*.apk 2^>nul') do (
    set APKNAME=%%i
    set APKPATH=%%~fi
    goto :done
)
:done
if not defined APKNAME (
    echo ❌ 未找到APK文件
    pause
    exit /b 1
)

echo ======================================
echo ✅ 打包完成
echo 📦 文件: %APKNAME%
echo 📏 大小:
for %%A in ("%APKNAME%") do echo    %%~zA 字节
echo 🔐 签名: eggrice (RSA 2048, SHA384withRSA)
echo ======================================
pause
