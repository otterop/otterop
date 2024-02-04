add_library(otterop_lang STATIC
${CMAKE_CURRENT_LIST_DIR}/error.c
${CMAKE_CURRENT_LIST_DIR}/generic.c
${CMAKE_CURRENT_LIST_DIR}/result.c
)
include(${CMAKE_CURRENT_LIST_DIR}/CMakeLists.manual.txt OPTIONAL)
