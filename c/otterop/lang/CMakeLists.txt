add_library(otterop_lang STATIC otterop/lang/string.c otterop/lang/array.c)
find_static_library(gc GC)
target_link_libraries(otterop_lang ${GC} pthread dl)