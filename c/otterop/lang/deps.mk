OTTEROP_LANG_STRING_OBJS:=otterop/lang/string.o
OTTEROP_LANG_ARRAY_OBJS:=otterop/lang/array.o $(OTTEROP_LANG_STRING_OBJS)

OTTEROP_LANG_LDFLAGS=
OTTEROP_LANG_LIBRARIES=
OTTEROP_LANG_INCLUDES=
OTTEROP_LANG_DEPENDENCIES=

otterop/lang/array.o: otterop/lang/string.o
