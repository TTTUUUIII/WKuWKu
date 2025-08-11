@echo off

setlocal
set prefix=plug-
if exist ".\build\outputs\apk\release" (
    rmdir /s /q ".\build\outputs\apk\release"
)

for %%m in (
    pcsx
    fmsx
    fbneo
    mesen
    bsnes
    snes9x
    citra
    melonds
    mgba
    vbam
    vbanext
    sameboy
    pokemini
    geargrafx
    prosystem
    yabause
    beetlesaturn
    mame2003plus
    genesisplusgx
) do (
    .\gradlew.bat :%prefix%%%m:assembleRelease
)