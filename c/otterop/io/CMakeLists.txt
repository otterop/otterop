add_library(otterop_io STATIC otterop/io/console.c)
include(./otterop/lang/CMakeLists.txt)
target_link_libraries(otterop_io gc pthread dl)