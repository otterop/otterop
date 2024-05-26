project(example_sort)

cmake_minimum_required(VERSION 3.16)
set(CMAKE_BINARY_DIR out)
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/lib)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)

function(find_static_library LIB_NAME OUT)

    if (WIN32 OR MSVC)
        set(CMAKE_FIND_LIBRARY_SUFFIXES ".lib")
    elseif (UNIX)
        set(CMAKE_FIND_LIBRARY_SUFFIXES ".a")
    endif()

    find_library(
        FOUND_${LIB_NAME}_STATIC
        ${LIB_NAME}
        )

    if (FOUND_${LIB_NAME}_STATIC)
        get_filename_component(ABS_FILE ${FOUND_${LIB_NAME}_STATIC} ABSOLUTE)
    else()
        message(SEND_ERROR "Unable to find library ${LIB_NAME}")
    endif()

    set(${OUT} ${ABS_FILE} PARENT_SCOPE)

endfunction()

include(./otterop/lang/CMakeLists.txt)
include(./otterop/datastructure/CMakeLists.txt)
include(./otterop/io/CMakeLists.txt)
include(./otterop/test/CMakeLists.txt)
include(./test/CMakeLists.txt)
find_static_library(gc GC)
set(OTTEROP_TESTS_LIBS otterop_lang otterop_test otterop_datastructure ${GC} pthread dl)
target_link_libraries(otterop_datastructure otterop_lang)
target_link_libraries(otterop_datastructure_tests ${OTTEROP_TESTS_LIBS} unity)
set_property(TARGET otterop_datastructure_tests PROPERTY C_STANDARD 99)
add_custom_target(test ./out/bin/otterop_datastructure_tests)
include_directories(.)
