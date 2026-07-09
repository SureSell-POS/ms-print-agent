@echo off
REM ============================================================================
REM  SureSell Print Agent - launcher de conveniencia para Windows.
REM  Arranca el agente de impresion (http://localhost:8181). Requiere Java 17.
REM
REM  Uso:
REM    run-agent.bat                  -> impresora por nombre "SAT" o la default
REM    run-agent.bat "POS-58"         -> fuerza el fragmento de nombre de impresora
REM ============================================================================
setlocal

REM Carpeta de este .bat (permite doble clic desde cualquier lado).
set "AGENT_DIR=%~dp0"

REM Busca el JAR: 1) junto al .bat, 2) en ..\..\build\libs (repo).
set "JAR="
for %%F in ("%AGENT_DIR%ms-print-agent*.jar") do set "JAR=%%F"
if not defined JAR for %%F in ("%AGENT_DIR%..\..\build\libs\ms-print-agent*.jar") do set "JAR=%%F"

if not defined JAR (
  echo [ERROR] No se encontro ms-print-agent*.jar junto a este .bat ni en build\libs.
  echo         Copia el JAR aqui o corre "gradlew.bat clean bootJar".
  pause
  exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
  echo [ERROR] No se encontro "java" en el PATH. Instala Java 17 (adoptium.net).
  pause
  exit /b 1
)

set "PRINTER_ARG="
if not "%~1"=="" set "PRINTER_ARG=--printer.name=%~1"

echo Iniciando SureSell Print Agent en http://localhost:8181 ...
echo   JAR: %JAR%
if not "%~1"=="" echo   Impresora: %~1
echo (Deja esta ventana abierta. Ctrl+C para detener.)
java -jar "%JAR%" %PRINTER_ARG%

endlocal
