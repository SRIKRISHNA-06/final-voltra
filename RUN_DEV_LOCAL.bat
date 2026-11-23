@echo off
cd /d "%~dp0"
title VOLTRA EV Charging Platform (Local Dev Mode)
color 0B
echo =====================================================================
echo    VOLTRA - Starting in Local Dev Mode (H2 + Seeded Stations)
echo =====================================================================
echo Open: http://localhost:8080
echo.
call "%~dp0mvnw.cmd" spring-boot:run -Dspring-boot.run.profiles=dev
pause
