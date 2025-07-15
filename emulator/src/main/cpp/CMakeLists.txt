
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("wkuwku")
add_subdirectory(../../../../libretro-fceumm ${CMAKE_BINARY_DIR}/libretro-fceumm)
message(${CMAKE_BINARY_DIR})

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
include_directories(include)
add_library(fceumm SHARED
        GLUtils.cpp
        GLRenderer.cpp
        ink_snowland_wkuwku_EmulatorV2.cpp)
target_link_libraries(fceumm android log retro EGL GLESv3)
#macro(add_bridge_library name)
#    add_library(${name}-bridge SHARED ink_snowland_wkuwku_libretro_bridge.cpp)
#    target_link_libraries(${name}-bridge android log ${name})
#    string(TOUPPER ${name} tmp)
#    string(REPLACE "-" "_" tag ${tmp})
#    target_compile_definitions(${name}-bridge PRIVATE ${tag})
#endmacro()
#
#add_bridge_library(fceumm)
##add_bridge_library(mesen)
##add_bridge_library(mesen-s)
##add_bridge_library(genesis-plus-gx)
##add_bridge_library(pcsx)
##add_bridge_library(bsnes)
#
#foreach(lib IN ITEMS fceumm)
#    add_library(${lib} SHARED IMPORTED)
#    set_target_properties(${lib} PROPERTIES IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}/lib${lib}.so")
#endforeach()
