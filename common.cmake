set(MY_DIR ${CMAKE_CURRENT_LIST_DIR})
include_directories(${MY_DIR}/emulator/src/main/cpp/include)
find_package(games-frame-pacing REQUIRED CONFIG)
set(COMMON_SOURCE
        ${MY_DIR}/emulator/src/main/cpp/GLRenderer.cpp
        ${MY_DIR}/emulator/src/main/cpp/GLUtils.cpp
        ${MY_DIR}/emulator/src/main/cpp/AudioOutputStream.cpp
        ${MY_DIR}/emulator/src/main/cpp/include/Buffer.h
        ${MY_DIR}/emulator/src/main/cpp/ink_snowland_wkuwku_EmulatorV2.cpp
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
        aaudio
        games-frame-pacing::swappy_static
        log)
endmacro()