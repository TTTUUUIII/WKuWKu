@echo off

setlocal
set PREFIX=plug-
set OUT=.\build\outpus\apk\release
if exist %OUT% (
    rmdir /s /q "%OUT%"
)
for %%M in (
    beetlesaturn
    fbneo
    fmsx
    geargrafx
    genesisplusgx
    mame2003plus
    melonds
    mesen
    pcsx
    prosystem
    sameboy
    snes9x
) do (
    .\gradlew.bat :%PREFIX%%%M:assembleRelease
)