add_library(otterop_datastructure STATIC
${CMAKE_CURRENT_LIST_DIR}/linked_list.c
${CMAKE_CURRENT_LIST_DIR}/linked_list_node.c
${CMAKE_CURRENT_LIST_DIR}/list.c
${CMAKE_CURRENT_LIST_DIR}/test_internal.c
)
include(${CMAKE_CURRENT_LIST_DIR}/CMakeLists.manual.txt OPTIONAL)
