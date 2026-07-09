@echo off
REM ============================================================================
REM  Construye un instalador NATIVO de Windows con JRE embebido (jpackage).
REM  Asi el cliente NO necesita instalar Java aparte.
REM
REM  Requisitos para correr ESTE script (en la maquina de build, Windows):
REM    - JDK 17+ (incluye jpackage) en el PATH.
REM    - Para --type msi / exe: WiX Toolset 3.x instalado (wixtoolset.org).
REM      Si NO tienes WiX, usa el default 'app-image' (carpeta portable con .exe).
REM
REM  Uso:
REM    build-installer.bat            -> app-image (portable, sin WiX)
REM    build-installer.bat msi        -> instalador .msi (requiere WiX)
REM    build-installer.bat exe        -> instalador .exe (requiere WiX)
REM ============================================================================
setlocal
set "PKG_TYPE=%~1"
if "%PKG_TYPE%"=="" set "PKG_TYPE=app-image"

set "SCRIPT_DIR=%~dp0"
set "REPO_DIR=%SCRIPT_DIR%..\.."
pushd "%REPO_DIR%"

echo == Compilando el JAR (gradlew clean bootJar) ==
call gradlew.bat clean bootJar -x test || (echo [ERROR] Fallo el build & popd & exit /b 1)

set "JAR="
for %%F in ("build\libs\ms-print-agent-*.jar") do set "JAR=%%~nxF"
if not defined JAR (echo [ERROR] No se genero el JAR & popd & exit /b 1)

echo == jpackage (%PKG_TYPE%) ==
if not exist "build\installer" mkdir "build\installer"

jpackage ^
  --type %PKG_TYPE% ^
  --name SureSellPrintAgent ^
  --app-version 0.0.1 ^
  --vendor "SureSell" ^
  --input build\libs ^
  --main-jar %JAR% ^
  --main-class org.springframework.boot.loader.launch.JarLauncher ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --win-console ^
  --dest build\installer

if errorlevel 1 (
  echo [ERROR] jpackage fallo. Si pediste msi/exe, instala WiX Toolset 3.x o usa 'app-image'.
  popd & exit /b 1
)

echo.
echo == Listo. Artefacto en: %REPO_DIR%\build\installer ==
popd
endlocal
