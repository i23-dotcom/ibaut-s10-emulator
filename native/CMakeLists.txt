cmake_minimum_required(VERSION 3.22.1)

project(ibaut_vm_bridge)

add_library(
        ibaut_vm_bridge
        SHARED
        qemu_bridge.cpp
)

find_library(
        log-lib
        log
)

target_compile_features(
        ibaut_vm_bridge
        PRIVATE
        cxx_std_17
)

target_link_libraries(
        ibaut_vm_bridge
        ${log-lib}
)
