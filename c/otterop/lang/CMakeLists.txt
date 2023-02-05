add_library(otterop_lang STATIC otterop/lang/string.c otterop/lang/array.c)
target_link_libraries(otterop_lang gc pthread dl)