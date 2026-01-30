set(MY_DIR ${CMAKE_CURRENT_LIST_DIR})
include_directories(
        ${MY_DIR}/../libretro-common/include
        ${MY_DIR}/include
)
find_package(games-frame-pacing REQUIRED CONFIG)
find_package(oboe REQUIRED CONFIG)
set(COMMON_SOURCE
        ${MY_DIR}/GLContext.cpp
        ${MY_DIR}/GLRenderer.cpp
        ${MY_DIR}/GLUtils.cpp
        ${MY_DIR}/AudioOutputStream.cpp
        ${MY_DIR}/include/Buffer.h
        ${MY_DIR}/Utils.cpp
        ${MY_DIR}/ink_snowland_wkuwku_EmulatorV2.cpp
)

macro(add_emulator _name _main_class)
add_library(
        ${_name} SHARED
        ${COMMON_SOURCE}
)
target_compile_definitions(${_name} PRIVATE MAIN_CLASS="${_main_class}")
add_library(libretro SHARED IMPORTED)
set_target_properties(libretro PROPERTIES IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}/libretro.so")
target_link_libraries(${_name}
        # List libraries link to the target library
        libretro
        android
        EGL
        GLESv3
        oboe::oboe
        games-frame-pacing::swappy_static
        log)
endmacro()