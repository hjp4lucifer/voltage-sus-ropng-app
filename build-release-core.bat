@echo off
setlocal enabledelayedexpansion

rem =====================================================================
rem  release APK 打包核心脚本 (在 build-release.bat 打开的窗口中手动执行)
rem  产物: app/build/outputs/apk/release/app-release-<versionName>.apk
rem  说明:
rem    1. 校验 JAVA_HOME_8 环境(launcher 不校验, 在此兜底)
rem    2. 版本号自动从 app/build.gradle 的 versionName 解析
rem    3. 打包前删除旧的同名产物并确认删除成功, 防止拿到旧包
rem    4. 使用项目根目录下的 gradlew 打包
rem    5. 跑完自然结束, 窗口由 cmd /k 保持
rem =====================================================================

set "ERROR_FLAG=0"

rem ---- 定位项目根目录(脚本所在目录) ----
set "PROJECT_DIR=%~dp0"
cd /d "%PROJECT_DIR%"

echo [INFO] 项目目录: %PROJECT_DIR%
echo [INFO] 当前 JAVA_HOME: %JAVA_HOME%
echo.

rem =====================================================================
rem 1. 校验 JAVA_HOME_8
rem =====================================================================
if not defined JAVA_HOME_8 (
    echo [ERROR] 未检测到 JAVA_HOME_8 环境变量!
    echo [ERROR] 请先配置 JDK8 环境变量 JAVA_HOME_8 后再执行本脚本。
    set "ERROR_FLAG=1"
    goto :end
)
if not exist "%JAVA_HOME_8%\bin\java.exe" (
    echo [ERROR] JAVA_HOME_8 指向的路径无效: %JAVA_HOME_8%
    echo [ERROR] 请检查 JAVA_HOME_8 是否指向 JDK8 的安装目录。
    set "ERROR_FLAG=1"
    goto :end
)
set "JAVA_HOME=%JAVA_HOME_8%"
set "PATH=%JAVA_HOME_8%\bin;%PATH%"
echo [OK] 已切换到 JDK8: %JAVA_HOME_8%

rem =====================================================================
rem 2. 校验 gradlew.bat 是否存在
rem =====================================================================
if not exist "%PROJECT_DIR%gradlew.bat" (
    echo [ERROR] 未找到 gradlew.bat, 请确认在项目根目录下运行本脚本。
    set "ERROR_FLAG=1"
    goto :end
)

rem =====================================================================
rem 3. 从 app/build.gradle 解析 versionName
rem    注意: findstr 在反引号子进程中执行, 不能依赖 %变量%,
rem    因此使用相对路径(脚本已 cd 到项目根目录)
rem =====================================================================
set "GRADLE_FILE=%PROJECT_DIR%app\build.gradle"
if not exist "%GRADLE_FILE%" (
    echo [ERROR] 未找到 %GRADLE_FILE%
    set "ERROR_FLAG=1"
    goto :end
)

set "VERSION_NAME="
for /f "usebackq tokens=*" %%L in (`findstr /c:"versionName" app\build.gradle`) do (
    set "LINE=%%L"
)
if not defined LINE (
    echo [ERROR] 无法从 app/build.gradle 解析 versionName。
    set "ERROR_FLAG=1"
    goto :end
)

rem 提取 versionName "xxx" 中的 xxx
set "LINE=%LINE:*versionName=%"
set "LINE=%LINE:"=%"
for /f "tokens=1" %%V in ("%LINE%") do (
    set "VERSION_NAME=%%V"
)
if not defined VERSION_NAME (
    echo [ERROR] 解析 versionName 失败, 请检查 app/build.gradle 格式。
    set "ERROR_FLAG=1"
    goto :end
)
echo [OK] 版本号: %VERSION_NAME%

rem =====================================================================
rem 4. 定义产物输出目录并删除旧的同名产物
rem =====================================================================
set "OUT_DIR=%PROJECT_DIR%app\build\outputs\apk\release"
set "DEST_DIR=%PROJECT_DIR%release"
set "TARGET_APK=%DEST_DIR%\app-release-%VERSION_NAME%.apk"

rem 确保根目录 release/ 存在
if not exist "%DEST_DIR%" (
    mkdir "%DEST_DIR%"
)

if exist "%TARGET_APK%" (
    del /q "%TARGET_APK%"
    if exist "%TARGET_APK%" (
        echo [ERROR] 旧包删除失败, 可能被占用或权限不足: %TARGET_APK%
        set "ERROR_FLAG=1"
        goto :end
    ) else (
        echo [OK] 已删除旧包: %TARGET_APK%
    )
) else (
    echo [INFO] 目标产物不存在, 无需删除: %TARGET_APK%
)

rem =====================================================================
rem 5. 执行 Gradle 打包
rem =====================================================================
rem 限制打包时 Gradle JVM 内存(仅本次打包生效, 不污染全局配置)
rem 项目较小, 1G 堆已足够; 配合 --no-daemon 让进程用完即退, 减少系统内存压力
set "GRADLE_OPTS=%GRADLE_OPTS% -Xmx1024m"
echo [INFO] 开始打包 release (JVM 堆上限 1024m, 关闭 daemon), 请稍候...
call "%PROJECT_DIR%gradlew.bat" assembleRelease --no-daemon
set "GRADLE_OPTS="
if errorlevel 1 (
    echo [ERROR] Gradle 打包失败!
    set "ERROR_FLAG=1"
    goto :end
)

rem =====================================================================
rem 6. 将产物移动到根目录 release/ 并重命名为 app-release-<versionName>.apk
rem    优先找 app-release.apk; 若未配置签名则产物为 app-release-unsigned.apk
rem =====================================================================
set "RAW_APK=%OUT_DIR%\app-release.apk"
if not exist "%RAW_APK%" (
    set "RAW_APK=%OUT_DIR%\app-release-unsigned.apk"
)
if not exist "%RAW_APK%" (
    echo [ERROR] 未找到打包产物, 应生成 app-release.apk 或 app-release-unsigned.apk: %OUT_DIR%
    set "ERROR_FLAG=1"
    goto :end
)

move /y "%RAW_APK%" "%TARGET_APK%" >nul
if not exist "%TARGET_APK%" (
    echo [ERROR] 产物移动失败: %TARGET_APK%
    set "ERROR_FLAG=1"
    goto :end
)

rem =====================================================================
rem 7. 完成
rem =====================================================================
echo ==========================================================
echo [OK] 打包完成!
echo [OK] 产物路径: %TARGET_APK%
echo ==========================================================

:end
echo.
if "%ERROR_FLAG%"=="1" (
    echo [FAIL] 打包过程中出现错误, 请查看上方日志排查。
) else (
    echo [OK] 脚本执行结束。
)
echo.
endlocal
