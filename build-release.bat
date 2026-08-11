@echo off
setlocal

rem =====================================================================
rem  release APK 打包启动脚本 (launcher)
rem  功能: 加载 JDK8 环境, 并打开新的 CMD 窗口执行打包核心脚本
rem  说明:
rem    1. 校验 JAVA_HOME_8 并加载 JDK8 环境
rem    2. 环境加载失败时不直接退出, 而是在新窗口中显示错误信息
rem    3. 新窗口使用 cmd /k 保持打开, 便于查看打包结果和错误
rem  用法: 双击本脚本, 或命令行执行 build-release.bat
rem =====================================================================

rem ---- 定位项目根目录(脚本所在目录) ----
set "PROJECT_DIR=%~dp0"
cd /d "%PROJECT_DIR%"

rem =====================================================================
rem 1. 校验并加载 JDK8 环境
rem =====================================================================
set "JDK8_LOADED=0"

if not defined JAVA_HOME_8 (
    echo [ERROR] 未检测到 JAVA_HOME_8 环境变量!
    echo [ERROR] 请在系统环境变量中配置 JAVA_HOME_8 后重试。
    goto :launch
)
if not exist "%JAVA_HOME_8%\bin\java.exe" (
    echo [ERROR] JAVA_HOME_8 指向的路径无效: %JAVA_HOME_8%
    echo [ERROR] 请检查 JAVA_HOME_8 是否指向 JDK8 的安装目录。
    goto :launch
)

set "JAVA_HOME=%JAVA_HOME_8%"
set "PATH=%JAVA_HOME_8%\bin;%PATH%"
set "JDK8_LOADED=1"
echo [OK] JDK8 环境加载成功: %JAVA_HOME_8%

:launch
echo.

rem =====================================================================
rem 2. 打开新的 CMD 窗口执行打包核心脚本
rem    使用 cmd /k 保持窗口打开, 环境变量通过当前进程传递给子进程
rem =====================================================================
if "%JDK8_LOADED%"=="1" (
    echo [INFO] 正在打开打包窗口...
    start "release 打包" cmd /k "cd /d %PROJECT_DIR% && call build-release-core.bat"
) else (
    echo [INFO] 环境加载失败, 打开窗口显示错误信息...
    start "release 打包 - 环境错误" cmd /k "cd /d %PROJECT_DIR% && echo [ERROR] 打包前需要先配置 JAVA_HOME_8 环境变量, 请退出本窗口并完成配置后重试。 && pause"
)

echo.
echo [INFO] 已在新的 CMD 窗口中启动打包, 请到新窗口查看进度和结果。
echo [INFO] 本启动窗口即将关闭。

endlocal
