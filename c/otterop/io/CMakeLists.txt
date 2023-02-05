add_library(otterop_io STATIC otterop/io/console.c)
include(./otterop/lang/CMakeLists.txt)
find_static_library(gc GC)
target_link_libraries(otterop_io ${GC} pthread dl)