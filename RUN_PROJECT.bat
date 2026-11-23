@echo off
cd /d "%~dp0"
title VOLTRA EV Charging Platform (Production PostgreSQL Mode)
color 0B
echo =====================================================================
echo    VOLTRA - Production Mode (Requires DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET)
echo =====================================================================
call "%~dp0mvnw.cmd" spring-boot:run
pause
