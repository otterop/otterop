add_executable(otterop_datastructure_tests
${CMAKE_CURRENT_LIST_DIR}/test_list.c
${CMAKE_CURRENT_LIST_DIR}/__tests_main.c
)
include(${CMAKE_CURRENT_LIST_DIR}/CMakeLists.manual.tests.txt OPTIONAL)
