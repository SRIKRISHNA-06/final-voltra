@echo off
setlocal
set "DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
set "WRAPPER_DISTS=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\voltra"
set "MVN_HOME=%WRAPPER_DISTS%\apache-maven-3.9.9"
set "ZIP_FILE=%WRAPPER_DISTS%\maven.zip"

if not exist "%MVN_HOME%\bin\mvn.cmd" (
    echo [VOLTRA] First-time setup: Downloading portable Apache Maven 3.9.9...
    if not exist "%WRAPPER_DISTS%" mkdir "%WRAPPER_DISTS%"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -UseBasicParsing -Uri '%DIST_URL%' -OutFile '%ZIP_FILE%'; Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '%WRAPPER_DISTS%' -Force; Remove-Item '%ZIP_FILE%' -Force"
)

if not exist "%MVN_HOME%\bin\mvn.cmd" (
    echo [ERROR] Could not download portable Maven. Check internet connection and retry.
    exit /b 1
)

call "%MVN_HOME%\bin\mvn.cmd" %*
