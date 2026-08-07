cmake_minimum_required(VERSION 3.22.1)
project(rustedfabric_jvmhost LANGUAGES CXX)

add_library(rustedfabric_jvmhost SHARED rustedfabric_jvmhost.cpp)
target_compile_features(rustedfabric_jvmhost PRIVATE cxx_std_17)
target_link_libraries(rustedfabric_jvmhost PRIVATE android log dl)

add_library(rustedfabric_renderbridge SHARED rustedfabric_renderbridge.cpp)
target_compile_features(rustedfabric_renderbridge PRIVATE cxx_std_17)
target_link_libraries(rustedfabric_renderbridge PRIVATE android log dl EGL GLESv2)

add_library(pojavexec SHARED rustedfabric_pojavexec.cpp)
target_compile_features(pojavexec PRIVATE cxx_std_17)
target_link_libraries(pojavexec PRIVATE rustedfabric_renderbridge android log EGL)

if(NOT EXISTS "${RUSTED_FABRIC_ROCKET_SOURCE}/Build/CMakeLists.txt")
    message(FATAL_ERROR "Pinned libRocket source is unavailable: ${RUSTED_FABRIC_ROCKET_SOURCE}")
endif()
if(NOT EXISTS "${RUSTED_FABRIC_FREETYPE_SOURCE}/include/ft2build.h")
    message(FATAL_ERROR "Pinned FreeType headers are unavailable: ${RUSTED_FABRIC_FREETYPE_SOURCE}")
endif()
if(NOT EXISTS "${RUSTED_FABRIC_FREETYPE_LIBRARY}")
    message(FATAL_ERROR "Pinned FreeType ARM64 library is unavailable: ${RUSTED_FABRIC_FREETYPE_LIBRARY}")
endif()

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_PYTHON_BINDINGS OFF CACHE BOOL "" FORCE)
set(BUILD_SAMPLES OFF CACHE BOOL "" FORCE)
set(FREETYPE_INCLUDE_DIR_ft2build "${RUSTED_FABRIC_FREETYPE_SOURCE}/include" CACHE PATH "" FORCE)
set(FREETYPE_INCLUDE_DIR_freetype2 "${RUSTED_FABRIC_FREETYPE_SOURCE}/include" CACHE PATH "" FORCE)
set(FREETYPE_LIBRARY_RELEASE "${RUSTED_FABRIC_FREETYPE_LIBRARY}" CACHE FILEPATH "" FORCE)
set(FREETYPE_LIBRARY_DEBUG "${RUSTED_FABRIC_FREETYPE_LIBRARY}" CACHE FILEPATH "" FORCE)
add_subdirectory("${RUSTED_FABRIC_ROCKET_SOURCE}/Build" librocket-build EXCLUDE_FROM_ALL)

foreach(rocket_target RocketCore RocketControls)
    set_target_properties(${rocket_target} PROPERTIES POSITION_INDEPENDENT_CODE ON)
    target_compile_definitions(${rocket_target} PUBLIC ROCKET_ARCH_64)
    target_compile_options(${rocket_target} PRIVATE
            -Wno-error -Wno-deprecated-declarations -std=c++11)
endforeach()

add_library(rocketConnector SHARED rustedfabric_rocketconnector.cpp)
target_compile_features(rocketConnector PRIVATE cxx_std_17)
target_compile_options(rocketConnector PRIVATE -Wno-error=unused-parameter)
target_include_directories(rocketConnector PRIVATE
        "${RUSTED_FABRIC_ROCKET_SOURCE}/Include")
target_link_libraries(rocketConnector PRIVATE RocketCore RocketControls android log)
