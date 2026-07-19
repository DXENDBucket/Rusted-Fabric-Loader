cmake_minimum_required(VERSION 3.22.1)
project(rustedfabric_jvmhost LANGUAGES CXX)

add_library(rustedfabric_jvmhost SHARED rustedfabric_jvmhost.cpp)
target_compile_features(rustedfabric_jvmhost PRIVATE cxx_std_17)
target_link_libraries(rustedfabric_jvmhost PRIVATE android log dl)
