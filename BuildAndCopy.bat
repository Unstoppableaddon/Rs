@echo off
setlocal
REM Build the plugin jar
where mvn >nul 2>&1
if %errorlevel% neq 0 (
  echo Maven not found. Please install Maven and Java 17.
  pause
  exit /b 1
)
mvn -q -U clean package
if exist target\BattleRoyale-0.1.jar (
  echo Built target\BattleRoyale-0.1.jar
  set /p PLUGINS_DIR=Enter your server plugins folder path (e.g. C:\Servers\Paper\plugins): 
  if not "%PLUGINS_DIR%"=="" (
    copy /Y target\BattleRoyale-0.1.jar "%PLUGINS_DIR%\BattleRoyale-0.1.jar"
    echo Copied to %PLUGINS_DIR%\BattleRoyale-0.1.jar
  )
) else (
  echo Build failed. See Maven output above.
)
pause
