@echo off
title Rockstar Client GUI
echo Uruchamianie Rockstar Client GUI...
start javaw -jar RockstarGUI.jar
if %errorlevel% neq 0 (
    echo.
    echo Blad uruchomienia przez javaw, probuje przez java...
    java -jar RockstarGUI.jar
    pause
)
