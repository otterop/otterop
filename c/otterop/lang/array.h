#ifndef __otterop_lang_Array
#define __otterop_lang_Array
#include <stddef.h>
#include <stdlib.h>
#include <gc.h>
#include <otterop/lang/string.h>

typedef struct otterop_lang_Array_s otterop_lang_Array_t;

otterop_lang_Array_t *otterop_lang_Array_wrap(int wrapped_count, void **wrapped);

otterop_lang_Array_t *otterop_lang_Array_wrap_string(int wrapped_count, char **wrapped);

void *otterop_lang_Array_get(otterop_lang_Array_t *this, int i);

void otterop_lang_Array_set(otterop_lang_Array_t *this, int i, void* value);

int otterop_lang_Array_size(otterop_lang_Array_t *this);
#endif
