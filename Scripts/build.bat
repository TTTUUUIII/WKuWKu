@echo off

setlocal
set prefix=plug-
if exist ".\build\outpus\apk\release" (
    rmdir /s /q ".\build\outpus\apk\release"
)
for %%module in (
    pcsx
    fmsx
    fbneo
    mesen
    snes9x
    melonds
    sameboy
    geargrafx
    prosystem
    beetlesaturn
    mame2003plus
    genesisplusgx
) do (
    .\gradlew.bat :%prefix%%%module:assembleRelease
)