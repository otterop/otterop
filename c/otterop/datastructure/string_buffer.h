#ifndef __otterop_datastructure_StringBuffer
#define __otterop_datastructure_StringBuffer
#include <otterop/lang/oop_iterable.h>
#include <otterop/lang/string.h>

typedef struct otterop_lang_OOPIterable_s otterop_lang_OOPIterable_t;
typedef struct otterop_lang_String_s otterop_lang_String_t;

typedef struct otterop_datastructure_StringBuffer_s otterop_datastructure_StringBuffer_t;




otterop_datastructure_StringBuffer_t *otterop_datastructure_StringBuffer_new();

void otterop_datastructure_StringBuffer_add(otterop_datastructure_StringBuffer_t *this, otterop_lang_String_t *s);


otterop_lang_String_t *otterop_datastructure_StringBuffer_oop_string(otterop_datastructure_StringBuffer_t *this);

#endif
